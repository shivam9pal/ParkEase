package com.parkease.parkinglot.feign;

import com.parkease.parkinglot.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for Auth Service — retrieves user details (email, name, etc.)
 *
 * Configuration: FeignConfig applies JWT token injection for service-to-service
 * calls
 */
@FeignClient(
        name = "auth-service",
        url = "${feign.auth-service.url:http://auth-service:8001}",
        configuration = FeignConfig.class,
        fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{userId}")
    UserDetailDto getUserById(@PathVariable("userId") UUID userId);
}
