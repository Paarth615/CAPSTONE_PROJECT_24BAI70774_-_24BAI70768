package com.lifelink.dto;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.RequestStatus;
import com.lifelink.model.ResponseStatus;
import com.lifelink.model.Urgency;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public final class MiscDtos {

    private MiscDtos() {
    }

    public record RespondForm(@Size(max = 500, message = "Message can be up to 500 characters") String message) {
    }

    public record ResponseDto(
            Long id,
            Long requestId,
            String patientName,
            BloodGroup requestBloodGroup,
            String hospital,
            String requestCity,
            Urgency urgency,
            RequestStatus requestStatus,
            String requestContactPhone,
            Long donorId,
            String donorName,
            BloodGroup donorBloodGroup,
            String donorCity,
            String donorPhone,
            ResponseStatus status,
            String message,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record NotificationDto(Long id, String message, String link, boolean seen, Instant createdAt) {
    }

    public record NotificationsPage(long unread, java.util.List<NotificationDto> items) {
    }

    public record PublicStats(
            long donors,
            long availableDonors,
            long openRequests,
            long fulfilledRequests,
            long donationsCompleted,
            long cities,
            Map<String, Long> availableByGroup
    ) {
    }

    public record AdminStats(
            Map<String, Long> usersByRole,
            Map<String, Long> requestsByStatus,
            Map<String, Long> responsesByStatus,
            Map<String, Long> donorsByGroup,
            long totalUsers,
            long totalRequests
    ) {
    }
}
