package com.parkease.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request to send notification to a specific user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendToUserNotificationRequest {

    @NotNull(message = "managerId is required")
    private UUID managerId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 1000, message = "message must not exceed 1000 characters")
    private String message;

    @NotBlank(message = "notificationType is required")
    private String notificationType;

    // Optional: related ID (e.g., lotId)
    private UUID relatedId;

    // Optional: related type (e.g., LOT)
    private String relatedType;
}
