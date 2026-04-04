package com.parkease.notification.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailDto {

    private UUID userId;
    private String fullName;
    private String email;       // Used for EMAIL channel dispatch
    private String phone;       // Used for SMS channel; may be null
    private String role;        // DRIVER, MANAGER, ADMIN
    private boolean isActive;
}