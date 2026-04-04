package com.parkease.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID notificationId;
    private UUID recipientId;
    private String type;          // String for clean JSON serialization
    private String title;
    private String message;
    private String channel;       // APP, EMAIL, SMS
    private UUID relatedId;
    private String relatedType;   // BOOKING, PAYMENT
    private boolean isRead;
    private LocalDateTime sentAt;
}