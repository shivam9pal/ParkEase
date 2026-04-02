package com.parkease.auth.service;

import com.parkease.auth.dto.*;
import com.parkease.auth.entity.User;

import java.util.UUID;

public interface AuthService {

    UserProfileResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    boolean validateToken(String token);

    AuthResponse refreshToken(String token);

    UserProfileResponse getUserByEmail(String email);

    UserProfileResponse getUserById(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void deactivateAccount(UUID userId);
}
