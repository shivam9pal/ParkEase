package com.parkease.parkinglot.feign;

import com.parkease.parkinglot.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for Notification Service — sends email notifications
 *
 * Configuration: FeignConfig applies JWT token injection for service-to-service
 * calls
 */
@FeignClient(
        name = "notification-service",
        configuration = FeignConfig.class,
        fallback = NotificationServiceClientFallback.class
)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send-to-user")
    void sendToUser(@RequestBody SendToUserNotificationDto request);
}
