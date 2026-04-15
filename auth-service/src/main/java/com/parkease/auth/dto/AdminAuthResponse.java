package com.parkease.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminAuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private AdminProfileResponse admin;
}