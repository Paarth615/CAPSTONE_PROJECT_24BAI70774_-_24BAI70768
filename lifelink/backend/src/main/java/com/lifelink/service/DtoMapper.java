package com.lifelink.service;

import com.lifelink.dto.MiscDtos.NotificationDto;
import com.lifelink.dto.MiscDtos.ResponseDto;
import com.lifelink.dto.RequestDtos.RequestDto;
import com.lifelink.dto.UserDtos.UserDto;
import com.lifelink.model.*;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.util.GeoUtil;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    private final DonationResponseRepository responses;
    private final EligibilityService eligibility;

    public DtoMapper(DonationResponseRepository responses, EligibilityService eligibility) {
        this.responses = responses;
        this.eligibility = eligibility;
    }

    public UserDto toUserDto(User u) {
        boolean donor = u.getRole() == Role.DONOR;
        String reason = donor ? eligibility.ineligibleReason(u) : null;
        return new UserDto(
                u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone(), u.getCity(),
                u.getLatitude(), u.getLongitude(), u.getBloodGroup(), u.getDateOfBirth(), u.getWeightKg(),
                u.getLastDonationDate(), u.isAvailable(), u.isEnabled(),
                donor && reason == null, reason,
                donor ? eligibility.nextEligibleDate(u) : null,
                donor ? responses.countByDonorAndStatus(u, ResponseStatus.COMPLETED) : 0,
                u.getCreatedAt());
    }

    public RequestDto toRequestDto(BloodRequest r, User viewer) {
        Double distance = null;
        if (viewer.getLatitude() != null && viewer.getLongitude() != null
                && r.getLatitude() != null && r.getLongitude() != null) {
            distance = GeoUtil.round1(GeoUtil.distanceKm(viewer.getLatitude(), viewer.getLongitude(),
                    r.getLatitude(), r.getLongitude()));
        }
        boolean isDonor = viewer.getRole() == Role.DONOR;
        boolean compatible = isDonor && viewer.getBloodGroup() != null
                && viewer.getBloodGroup().canDonateTo(r.getBloodGroup());
        ResponseStatus mine = isDonor
                ? responses.findByRequestAndDonor(r, viewer).map(DonationResponse::getStatus).orElse(null)
                : null;
        return new RequestDto(
                r.getId(), r.getRequester().getId(), r.getRequester().getName(), r.getPatientName(),
                r.getBloodGroup(), r.getUnitsRequired(), r.getUnitsFulfilled(), r.getHospital(), r.getCity(),
                r.getLatitude(), r.getLongitude(), r.getUrgency(), r.getRequiredBy(), r.getContactPhone(),
                r.getNotes(), r.getStatus(), r.getCreatedAt(), responses.countByRequest(r), mine, compatible,
                distance);
    }

    public ResponseDto toResponseDto(DonationResponse x) {
        BloodRequest r = x.getRequest();
        User d = x.getDonor();
        return new ResponseDto(
                x.getId(), r.getId(), r.getPatientName(), r.getBloodGroup(), r.getHospital(), r.getCity(),
                r.getUrgency(), r.getStatus(), r.getContactPhone(),
                d.getId(), d.getName(), d.getBloodGroup(), d.getCity(), d.getPhone(),
                x.getStatus(), x.getMessage(), x.getCreatedAt(), x.getUpdatedAt());
    }

    public NotificationDto toNotificationDto(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.isSeen(), n.getCreatedAt());
    }
}
