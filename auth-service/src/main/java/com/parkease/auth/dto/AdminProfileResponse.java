package com.parkease.auth.dto;

import com.parkease.auth.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminProfileResponse {

    private UUID adminId;
    private String fullName;
    private String email;
    private User.Role role;

    // Boolean (boxed) → Lombok generates getIsActive() → Jackson serializes as "isActive" ✅
    private Boolean isActive;

    // Boolean (boxed) → Lombok generates getIsSuperAdmin() → Jackson serializes as "isSuperAdmin" ✅
    private Boolean isSuperAdmin;

    private LocalDateTime createdAt;
}
