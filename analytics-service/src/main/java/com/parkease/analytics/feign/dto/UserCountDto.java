package com.parkease.analytics.feign.dto;

import java.util.List;

import lombok.Data;

/**
 * DTO for user count response from auth-service. This actually returns a list
 * of UserProfileResponse from /api/v1/auth/users endpoint. We use this DTO just
 * to get the list and count its size.
 */
@Data
public class UserCountDto {

    private List<UserProfileDto> users;

    @Data
    public static class UserProfileDto {

        private String id;
        private String email;
        private String fullName;
        private String role;
        private boolean active;
    }
}
