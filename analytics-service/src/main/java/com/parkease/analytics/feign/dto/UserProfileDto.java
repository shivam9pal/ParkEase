package com.parkease.analytics.feign.dto;

import lombok.Data;

/**
 * DTO for individual user from auth-service. Represents a single user object
 * from /api/v1/auth/users endpoint. Auth-service returns an array of these
 * objects directly.
 */
@Data
public class UserProfileDto {

    private String id;
    private String email;
    private String fullName;
    private String role;        // DRIVER, MANAGER, ADMIN
    private boolean isActive;
}
