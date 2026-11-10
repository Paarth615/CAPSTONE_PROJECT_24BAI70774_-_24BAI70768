package com.lifelink.service;

import com.lifelink.dto.RequestDtos.MatchDto;
import com.lifelink.dto.RequestDtos.RequestDto;
import com.lifelink.dto.RequestDtos.RequestForm;
import com.lifelink.exception.ApiException;
import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);
    private static final int AUTO_NOTIFY_LIMIT = 10;

    private final BloodRequestRepository requests;
    private final DonationResponseRepository responses;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final MatchingService matching;
    private final NotificationService notifications;
    private final DtoMapper mapper;

    public RequestService(BloodRequestRepository requests, DonationResponseRepository responses,
                          UserRepository users, CurrentUser currentUser, MatchingService matching,
                          NotificationService notifications, DtoMapper mapper) {
        this.requests = requests;
        this.responses = responses;
        this.users = users;
        this.currentUser = currentUser;
        this.matching = matching;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    public RequestDto create(RequestForm f) {
        User user = currentUser.get();
        if (f.requiredBy().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("The required-by date cannot be in the past");
        }
        if (f.requiredBy().isAfter(LocalDate.now().plusDays(90))) {
            throw ApiException.badRequest("The required-by date must be within the next 90 days");
        }

        BloodRequest r = new BloodRequest();
        r.setRequester(user);
        r.setPatientName(f.patientName().trim());
        r.setBloodGroup(f.bloodGroup());
        r.setUnitsRequired(f.unitsRequired());
        r.setHospital(f.hospital().trim());
        r.setCity(f.city().trim());
        r.setLatitude(f.latitude());
        r.setLongitude(f.longitude());
        r.setUrgency(f.urgency());
        r.setRequiredBy(f.requiredBy());
        r.setContactPhone(f.contactPhone());
        r.setNotes(f.notes() == null || f.notes().isBlank() ? null : f.notes().trim());
        r = requests.save(r);

        int notified = notifyTopMatches(r, AUTO_NOTIFY_LIMIT);
        log.info("Blood request {} created ({} {}), notified {} matching donors",
                r.getId(), r.getBloodGroup().getLabel(), r.getUrgency(), notified);
        return mapper.toRequestDto(r, user);
    }

    @Transactional(readOnly = true)
    public List<RequestDto> list(String blood, String city, String urgency, boolean forMe) {
        User viewer = currentUser.get();
        LocalDate today = LocalDate.now();

        BloodGroup group = blood == null || blood.isBlank() ? null : BloodGroup.fromLabel(blood);
        Urgency urg = null;
        if (urgency != null && !urgency.isBlank()) {
            try {
                urg = Urgency.valueOf(urgency.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Unknown urgency: " + urgency);
            }
        }
        final BloodGroup groupFilter = group;
        final Urgency urgencyFilter = urg;

        Stream<BloodRequest> stream = requests.findByStatus(RequestStatus.OPEN).stream()
                .filter(r -> !r.getRequiredBy().isBefore(today));
        if (groupFilter != null) {
            stream = stream.filter(r -> r.getBloodGroup() == groupFilter);
        }
        if (urgencyFilter != null) {
            stream = stream.filter(r -> r.getUrgency() == urgencyFilter);
        }
        if (city != null && !city.isBlank()) {
            String needle = city.trim().toLowerCase();
            stream = stream.filter(r -> r.getCity().toLowerCase().contains(needle));
        }
        if (forMe) {
            stream = stream.filter(r -> !r.getRequester().getId().equals(viewer.getId())
                    && viewer.getBloodGroup() != null
                    && viewer.getBloodGroup().canDonateTo(r.getBloodGroup()));
        }

        return stream
                .sorted(Comparator.comparingInt((BloodRequest r) -> r.getUrgency().getWeight()).reversed()
                        .thenComparing(BloodRequest::getRequiredBy)
                        .thenComparing(BloodRequest::getCreatedAt, Comparator.reverseOrder()))
                .map(r -> mapper.toRequestDto(r, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDto get(Long id) {
        return mapper.toRequestDto(find(id), currentUser.get());
    }

    @Transactional(readOnly = true)
    public List<RequestDto> mine() {
        User user = currentUser.get();
        return requests.findByRequesterOrderByCreatedAtDesc(user).stream()
                .map(r -> mapper.toRequestDto(r, user)).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchDto> matches(Long id) {
        BloodRequest r = find(id);
        assertOwnerOrAdmin(currentUser.get(), r);
        if (r.getStatus() != RequestStatus.OPEN) {
            return List.of();
        }
        return matching.findMatches(r, 20);
    }

    /** Sends an in-app alert to the best-matching donors again (a reminder). Returns how many were alerted. */
    public int notifyMatches(Long id) {
        BloodRequest r = find(id);
        assertOwnerOrAdmin(currentUser.get(), r);
        if (r.getStatus() != RequestStatus.OPEN) {
            throw ApiException.badRequest("Only open requests can be sent to donors");
        }
        return notifyTopMatches(r, AUTO_NOTIFY_LIMIT);
    }

    public RequestDto updateStatus(Long id, String statusText) {
        User user = currentUser.get();
        BloodRequest r = find(id);
        assertOwnerOrAdmin(user, r);

        RequestStatus target;
        try {
            target = RequestStatus.valueOf(statusText.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown status: " + statusText);
        }
        if (target != RequestStatus.CANCELLED && target != RequestStatus.FULFILLED) {
            throw ApiException.badRequest("You can only cancel a request or mark it fulfilled");
        }
        if (r.getStatus() != RequestStatus.OPEN) {
            throw ApiException.badRequest("This request is already " + r.getStatus().name().toLowerCase());
        }

        r.setStatus(target);
        requests.save(r);
        String why = target == RequestStatus.CANCELLED
                ? "was cancelled by the requester" : "has been fulfilled. Thank you for stepping up";
        closeOpenResponses(r, "The request for " + r.getPatientName() + " " + why + ".");
        log.info("Request {} set to {} by user {}", id, target, user.getId());
        return mapper.toRequestDto(r, user);
    }

    /** Admin moderation: removes a request and its responses. */
    public void adminDelete(Long id) {
        BloodRequest r = find(id);
        responses.deleteAll(responses.findByRequestOrderByCreatedAtDesc(r));
        requests.delete(r);
        log.info("Request {} deleted by admin", id);
    }

    /** Hourly sweep: open requests whose required-by date has passed become EXPIRED. */
    @Scheduled(fixedRate = 3_600_000, initialDelay = 30_000)
    public void expireOverdue() {
        List<BloodRequest> overdue = requests.findByStatusAndRequiredByBefore(RequestStatus.OPEN, LocalDate.now());
        for (BloodRequest r : overdue) {
            r.setStatus(RequestStatus.EXPIRED);
            requests.save(r);
            closeOpenResponses(r, "The request for " + r.getPatientName() + " has expired.");
            notifications.notify(r.getRequester(),
                    "Your request for " + r.getBloodGroup().getLabel() + " blood for " + r.getPatientName()
                            + " has expired. Raise a new one if blood is still needed.",
                    "/requests/" + r.getId());
        }
        if (!overdue.isEmpty()) {
            log.info("Expired {} overdue blood requests", overdue.size());
        }
    }

    // ---------------------------------------------------------------- helpers

    private BloodRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Blood request not found"));
    }

    private void assertOwnerOrAdmin(User user, BloodRequest r) {
        boolean owner = r.getRequester().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the person who raised this request can do that");
        }
    }

    private void closeOpenResponses(BloodRequest r, String message) {
        for (DonationResponse resp : responses.findByRequestOrderByCreatedAtDesc(r)) {
            if (resp.getStatus() == ResponseStatus.PENDING || resp.getStatus() == ResponseStatus.ACCEPTED) {
                resp.setStatus(ResponseStatus.DECLINED);
                resp.setUpdatedAt(java.time.Instant.now());
                responses.save(resp);
                notifications.notify(resp.getDonor(), message, "/requests/" + r.getId());
            }
        }
    }

    private int notifyTopMatches(BloodRequest r, int limit) {
        List<MatchDto> top = matching.findMatches(r, limit);
        String text = alertText(r);
        for (MatchDto m : top) {
            users.findById(m.donorId()).ifPresent(d -> notifications.notify(d, text, "/requests/" + r.getId()));
        }
        return top.size();
    }

    private String alertText(BloodRequest r) {
        String lead = switch (r.getUrgency()) {
            case CRITICAL -> "Critical";
            case URGENT -> "Urgent";
            default -> "Blood request";
        };
        int u = r.getUnitsRequired();
        return lead + ": " + u + " unit" + (u == 1 ? "" : "s") + " of " + r.getBloodGroup().getLabel()
                + " needed at " + r.getHospital() + ", " + r.getCity() + ". You are a match.";
    }
}
