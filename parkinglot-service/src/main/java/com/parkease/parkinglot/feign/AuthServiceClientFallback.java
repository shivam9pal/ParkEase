package com.parkease.parkinglot.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation when auth-service is unavailable
 */
@Slf4j
@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public UserDetailDto getUserById(UUID userId) {
        log.warn("Auth service is unavailable, returning fallback for user: {}", userId);
        return null;
    }
}
