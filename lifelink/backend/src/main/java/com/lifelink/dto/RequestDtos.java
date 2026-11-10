package com.lifelink.dto;

import com.lifelink.model.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RequestDtos {

    private RequestDtos() {
    }

    public record RequestForm(
            @NotBlank(message = "Patient name is required") @Size(max = 80) String patientName,
            @NotNull(message = "Choose the blood group needed") BloodGroup bloodGroup,
            @Min(value = 1, message = "At least 1 unit is needed")
            @Max(value = 10, message = "You can request up to 10 units at a time") int unitsRequired,
            @NotBlank(message = "Hospital name is required") @Size(max = 120) String hospital,
            @NotBlank(message = "City is required") @Size(max = 60) String city,
            Double latitude,
            Double longitude,
            @NotNull(message = "Choose an urgency level") Urgency urgency,
            @NotNull(message = "Choose the date blood is needed by") LocalDate requiredBy,
            @NotBlank(message = "Contact phone is required")
            @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Enter a 10 to 13 digit phone number") String contactPhone,
            @Size(max = 1000, message = "Notes can be up to 1000 characters") String notes
    ) {
    }

    public record StatusUpdate(@NotNull(message = "Status is required") String status) {
    }

    /** Request as shown in lists and on the detail page. */
    public record RequestDto(
            Long id,
            Long requesterId,
            String requesterName,
            String patientName,
            BloodGroup bloodGroup,
            int unitsRequired,
            int unitsFulfilled,
            String hospital,
            String city,
            Double latitude,
            Double longitude,
            Urgency urgency,
            LocalDate requiredBy,
            String contactPhone,
            String notes,
            RequestStatus status,
            Instant createdAt,
            long responseCount,
            ResponseStatus myResponseStatus,
            boolean compatibleWithMe,
            Double distanceKm
    ) {
    }

    /** One ranked donor suggestion for a request. */
    public record MatchDto(
            Long donorId,
            String name,
            BloodGroup bloodGroup,
            String city,
            String phone,
            int score,
            Double distanceKm,
            LocalDate lastDonationDate,
            long completedDonations,
            boolean exactMatch,
            List<String> reasons
    ) {
    }
}
