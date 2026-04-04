package com.parkease.notification.feign;

import com.parkease.notification.feign.dto.UserDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "auth-service",
        url = "${services.auth.url}",
        configuration = FeignConfig.class
)
public interface AuthServiceClient {

    /**
     * Fetch a single user's contact info (email, phone, fullName) for notification dispatch.
     * Called from RabbitMQ consumer threads using system JWT.
     */
    @GetMapping("/api/v1/auth/users/{userId}")
    UserDetailDto getUserById(@PathVariable("userId") UUID userId);

    /**
     * Fetch all users by role — used for PROMO broadcast.
     * Pass "ALL" to get every user regardless of role.
     */
    @GetMapping("/api/v1/auth/users")
    List<UserDetailDto> getUsersByRole(@RequestParam("role") String role);
}