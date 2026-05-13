package com.parkease.parkinglot.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation when notification-service is unavailable
 */
@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void sendToUser(SendToUserNotificationDto request) {
        log.warn("Notification service is unavailable, failed to send to: {}", request.getManagerId());
        // In production, could queue this for retry or log to external service
    }
}
