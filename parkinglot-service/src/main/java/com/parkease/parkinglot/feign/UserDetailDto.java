package com.parkease.parkinglot.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * User details from Auth Service
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailDto {

    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean isActive;
}
