package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.Role;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 80) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters") String password,
            @NotNull(message = "Choose how you want to use LifeLink") Role role,
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Enter a 10 to 13 digit phone number") String phone,
            @NotBlank(message = "City is required") @Size(max = 60) String city,
            Double latitude,
            Double longitude,
            BloodGroup bloodGroup,
            LocalDate dateOfBirth,
            Double weightKg
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password
    ) {
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
