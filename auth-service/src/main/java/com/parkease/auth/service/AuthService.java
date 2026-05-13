package com.parkease.auth.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.parkease.auth.dto.AdminAuthResponse;
import com.parkease.auth.dto.AdminCreateRequest;
import com.parkease.auth.dto.AdminLoginRequest;
import com.parkease.auth.dto.AdminProfileResponse;
import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.ChangePasswordRequest;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.OtpSendRequest;
import com.parkease.auth.dto.OtpVerifyRequest;
import com.parkease.auth.dto.RegisterRequest;
import com.parkease.auth.dto.ResetPasswordRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.dto.UserDetailDto;
import com.parkease.auth.dto.UserProfileResponse;
import com.parkease.auth.entity.User;

public interface AuthService {

    // ── Existing methods (UNCHANGED) ──────────────────────────────────────────
    UserProfileResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    boolean validateToken(String token);

    AuthResponse refreshToken(String token);

    UserProfileResponse getUserByEmail(String email);

    UserProfileResponse getUserById(UUID userId);

    /**
     * Get lightweight user details by userId for inter-service communication.
     * Called by notification-service, analytics-service, etc. No authorization
     * required when called with system JWT.
     */
    UserDetailDto getUserDetailById(UUID userId);

    /**
     * Get lightweight user details for all users with a specific role. Useful
     * for broadcast notifications to drivers, managers, etc. No authorization
     * required when called with system JWT.
     */
    List<UserDetailDto> getUserDetailsByRole(User.Role role);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserProfileResponse uploadAndUpdateProfilePicture(UUID userId, org.springframework.web.multipart.MultipartFile file);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void deactivateAccount(UUID userId);

    // ── New User Management methods (Admin only) ──────────────────────────────
    List<UserProfileResponse> getAllUsers(Optional<User.Role> roleFilter);

    UserProfileResponse deactivateUserAsAdmin(UUID userId);

    UserProfileResponse reactivateUserAsAdmin(UUID userId);

    // ── New OTP methods ───────────────────────────────────────────────────────
    String sendOtp(OtpSendRequest request);

    String verifyOtp(OtpVerifyRequest request);

    void resetPassword(ResetPasswordRequest request);

    // ── New Admin methods ─────────────────────────────────────────────────────
    AdminAuthResponse adminLogin(AdminLoginRequest request);

    AdminProfileResponse createAdmin(AdminCreateRequest request, UUID requesterId);

    void deleteAdmin(UUID targetAdminId, UUID requesterId);

    AdminProfileResponse reactivateAdmin(UUID targetAdminId, UUID requesterId);

    List<AdminProfileResponse> getAllAdmins(UUID requesterId);
}
