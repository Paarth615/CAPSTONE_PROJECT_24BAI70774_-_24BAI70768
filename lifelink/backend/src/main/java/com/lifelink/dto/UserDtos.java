package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.Role;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(
            Long id,
            String name,
            String email,
            Role role,
            String phone,
            String city,
            Double latitude,
            Double longitude,
            BloodGroup bloodGroup,
            LocalDate dateOfBirth,
            Double weightKg,
            LocalDate lastDonationDate,
            boolean available,
            boolean enabled,
            boolean eligible,
            String eligibilityNote,
            LocalDate nextEligibleDate,
            long completedDonations,
            Instant createdAt
    ) {
    }

    public record ProfileUpdate(
            @NotBlank(message = "Name is required") @Size(max = 80) String name,
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Enter a 10 to 13 digit phone number") String phone,
            @NotBlank(message = "City is required") @Size(max = 60) String city,
            Double latitude,
            Double longitude,
            BloodGroup bloodGroup,
            LocalDate dateOfBirth,
            Double weightKg,
            @PastOrPresent(message = "Last donation date cannot be in the future") LocalDate lastDonationDate
    ) {
    }

    public record AvailabilityUpdate(boolean available) {
    }

    public record EnabledUpdate(boolean enabled) {
    }
}
