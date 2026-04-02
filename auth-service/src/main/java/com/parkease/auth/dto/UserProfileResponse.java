package com.parkease.auth.dto;

import com.parkease.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String vehiclePlate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String profilePicUrl;
}
