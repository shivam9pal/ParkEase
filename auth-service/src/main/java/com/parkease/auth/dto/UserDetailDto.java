package com.parkease.auth.dto;

import com.parkease.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight DTO for inter-service communication (notification, analytics,
 * etc.). Contains only essential user info needed by other services. Maps to:
 * {@link com.parkease.auth.entity.User}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailDto {

    private UUID userId;
    private String fullName;
    private String email;              // Used by notification-service for email dispatch
    private String phone;              // Used by notification-service for SMS dispatch (may be null)
    private User.Role role;            // DRIVER, MANAGER, ADMIN
    private boolean isActive;
}
