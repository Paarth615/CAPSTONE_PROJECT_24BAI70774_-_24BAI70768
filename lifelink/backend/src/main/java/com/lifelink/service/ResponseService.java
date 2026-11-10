package com.lifelink.service;

import com.lifelink.dto.MiscDtos.RespondForm;
import com.lifelink.dto.MiscDtos.ResponseDto;
import com.lifelink.exception.ApiException;
import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The donor/requester handshake: PENDING -> ACCEPTED -> COMPLETED, with DECLINED (requester)
 * and WITHDRAWN (donor) as the exits.
 */
@Service
@Transactional
public class ResponseService {

    private static final Logger log = LoggerFactory.getLogger(ResponseService.class);

    private final DonationResponseRepository responses;
    private final BloodRequestRepository requests;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final EligibilityService eligibility;
    private final NotificationService notifications;
    private final DtoMapper mapper;

    public ResponseService(DonationResponseRepository responses, BloodRequestRepository requests,
                           UserRepository users, CurrentUser currentUser, EligibilityService eligibility,
                           NotificationService notifications, DtoMapper mapper) {
        this.responses = responses;
        this.requests = requests;
        this.users = users;
        this.currentUser = currentUser;
        this.eligibility = eligibility;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    public ResponseDto respond(Long requestId, RespondForm form) {
        User donor = currentUser.get();
        if (donor.getRole() != Role.DONOR) {
            throw ApiException.forbidden("Only donors can offer to donate");
        }
        BloodRequest req = requests.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Blood request not found"));

        if (req.getStatus() != RequestStatus.OPEN || req.getRequiredBy().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("This request is no longer open");
        }
        if (req.getRequester().getId().equals(donor.getId())) {
            throw ApiException.badRequest("You cannot respond to your own request");
        }
        String reason = eligibility.ineligibleReason(donor);
        if (reason != null) {
            throw ApiException.badRequest(reason);
        }
        if (!donor.getBloodGroup().canDonateTo(req.getBloodGroup())) {
            throw ApiException.badRequest("Your blood group (" + donor.getBloodGroup().getLabel()
                    + ") cannot donate to a " + req.getBloodGroup().getLabel() + " patient");
        }

        String message = form == null || form.message() == null || form.message().isBlank()
                ? null : form.message().trim();

        DonationResponse resp = responses.findByRequestAndDonor(req, donor).orElse(null);
        if (resp != null) {
            if (resp.getStatus() != ResponseStatus.WITHDRAWN) {
                throw ApiException.conflict("You have already responded to this request");
            }
            resp.setStatus(ResponseStatus.PENDING); // donor changed their mind and re-offers
            resp.setMessage(message);
            resp.setUpdatedAt(Instant.now());
        } else {
            resp = new DonationResponse();
            resp.setRequest(req);
            resp.setDonor(donor);
            resp.setMessage(message);
        }
        resp = responses.save(resp);

        notifications.notify(req.getRequester(),
                donor.getName() + " (" + donor.getBloodGroup().getLabel() + ") offered to donate for "
                        + req.getPatientName() + ".",
                "/requests/" + req.getId());
        log.info("Donor {} responded to request {}", donor.getId(), req.getId());
        return mapper.toResponseDto(resp);
    }

    @Transactional(readOnly = true)
    public List<ResponseDto> mine() {
        User donor = currentUser.get();
        return responses.findByDonorOrderByCreatedAtDesc(donor).stream().map(mapper::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ResponseDto> forRequest(Long requestId) {
        User user = currentUser.get();
        BloodRequest req = requests.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Blood request not found"));
        if (!req.getRequester().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the person who raised this request can see its responses");
        }
        return responses.findByRequestOrderByCreatedAtDesc(req).stream().map(mapper::toResponseDto).toList();
    }

    public ResponseDto updateStatus(Long responseId, String statusText) {
        User user = currentUser.get();
        DonationResponse resp = responses.findById(responseId)
                .orElseThrow(() -> ApiException.notFound("Response not found"));
        BloodRequest req = resp.getRequest();

        ResponseStatus target;
        try {
            target = ResponseStatus.valueOf(statusText.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown status: " + statusText);
        }

        boolean isOwner = req.getRequester().getId().equals(user.getId()) || user.getRole() == Role.ADMIN;
        boolean isDonor = resp.getDonor().getId().equals(user.getId());
        ResponseStatus current = resp.getStatus();

        switch (target) {
            case ACCEPTED -> {
                requireOwner(isOwner);
                requireStatus(current, ResponseStatus.PENDING);
                requireOpen(req);
                resp.setStatus(ResponseStatus.ACCEPTED);
                notifications.notify(resp.getDonor(),
                        "Your offer was accepted. Please contact " + req.getHospital() + " on "
                                + req.getContactPhone() + " to arrange the donation for " + req.getPatientName() + ".",
                        "/requests/" + req.getId());
            }
            case DECLINED -> {
                requireOwner(isOwner);
                requireStatus(current, ResponseStatus.PENDING);
                resp.setStatus(ResponseStatus.DECLINED);
                notifications.notify(resp.getDonor(),
                        "Thank you for offering. The request for " + req.getPatientName()
                                + " was matched with someone else.",
                        "/requests/" + req.getId());
            }
            case COMPLETED -> {
                requireOwner(isOwner);
                requireStatus(current, ResponseStatus.ACCEPTED);
                requireOpen(req);
                completeDonation(resp, req);
            }
            case WITHDRAWN -> {
                if (!isDonor) {
                    throw ApiException.forbidden("Only the donor can withdraw an offer");
                }
                if (current != ResponseStatus.PENDING && current != ResponseStatus.ACCEPTED) {
                    throw ApiException.badRequest("This offer can no longer be withdrawn");
                }
                resp.setStatus(ResponseStatus.WITHDRAWN);
                notifications.notify(req.getRequester(),
                        resp.getDonor().getName() + " withdrew their offer for " + req.getPatientName() + ".",
                        "/requests/" + req.getId());
            }
            default -> throw ApiException.badRequest("That status change is not allowed");
        }

        resp.setUpdatedAt(Instant.now());
        responses.save(resp);
        log.info("Response {} moved {} -> {} by user {}", responseId, current, target, user.getId());
        return mapper.toResponseDto(resp);
    }

    /** Records the donation: updates the donor's cool-down clock and the request's progress. */
    private void completeDonation(DonationResponse resp, BloodRequest req) {
        resp.setStatus(ResponseStatus.COMPLETED);

        User donor = resp.getDonor();
        donor.setLastDonationDate(LocalDate.now());
        users.save(donor);

        req.setUnitsFulfilled(req.getUnitsFulfilled() + 1);
        boolean done = req.getUnitsFulfilled() >= req.getUnitsRequired();
        if (done) {
            req.setStatus(RequestStatus.FULFILLED);
        }
        requests.save(req);

        notifications.notify(donor,
                "Thank you! Your donation for " + req.getPatientName() + " is recorded. You can donate again on "
                        + eligibility.nextEligibleDate(donor) + ".",
                "/requests/" + req.getId());

        if (done) {
            for (DonationResponse other : responses.findByRequestOrderByCreatedAtDesc(req)) {
                boolean stillOpen = other.getStatus() == ResponseStatus.PENDING
                        || other.getStatus() == ResponseStatus.ACCEPTED;
                if (!other.getId().equals(resp.getId()) && stillOpen) {
                    other.setStatus(ResponseStatus.DECLINED);
                    other.setUpdatedAt(Instant.now());
                    responses.save(other);
                    notifications.notify(other.getDonor(),
                            "The request for " + req.getPatientName()
                                    + " is now fulfilled. Thank you for offering to help.",
                            "/requests/" + req.getId());
                }
            }
        }
    }

    private void requireOwner(boolean isOwner) {
        if (!isOwner) {
            throw ApiException.forbidden("Only the person who raised this request can do that");
        }
    }

    private void requireStatus(ResponseStatus actual, ResponseStatus expected) {
        if (actual != expected) {
            throw ApiException.badRequest("This response is " + actual.name().toLowerCase()
                    + ", so that change is not allowed");
        }
    }

    private void requireOpen(BloodRequest req) {
        if (req.getStatus() != RequestStatus.OPEN) {
            throw ApiException.badRequest("This request is no longer open");
        }
    }
}
