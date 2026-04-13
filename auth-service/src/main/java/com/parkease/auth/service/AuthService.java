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
import com.parkease.auth.dto.UserProfileResponse;
import com.parkease.auth.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    // ── Existing methods (UNCHANGED) ──────────────────────────────────────────
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

    // ── Media/S3 Upload methods ───────────────────────────────────────────────
    /**
     * Upload user profile picture to S3 via media-service
     *
     * @param userId The user uploading the picture
     * @param file The image file to upload
     * @return The S3 URL of the uploaded image
     */
    String uploadProfilePicture(UUID userId, MultipartFile file);
}
