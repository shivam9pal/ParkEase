package com.parkease.parkinglot.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for sending notifications to a specific user via notification-service
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendToUserNotificationDto {

    private UUID managerId;
    private String title;
    private String message;
    private String notificationType;
    private UUID relatedId;      // lotId
    private String relatedType;  // "LOT"
}
