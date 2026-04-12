package com.parkease.auth.resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.auth.dto.AdminAuthResponse;
import com.parkease.auth.dto.AdminCreateRequest;
import com.parkease.auth.dto.AdminLoginRequest;
import com.parkease.auth.dto.AdminProfileResponse;
import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.ChangePasswordRequest;
import com.parkease.auth.dto.ForgotPasswordRequest;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.OtpSendRequest;
import com.parkease.auth.dto.OtpVerifyRequest;
import com.parkease.auth.dto.RefreshTokenRequest;
import com.parkease.auth.dto.RegisterRequest;
import com.parkease.auth.dto.ResetPasswordRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.dto.UserProfileResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.security.JwtUtil;
import com.parkease.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management endpoints")
public class AuthResource {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING ENDPOINTS — completely unchanged
    // ═══════════════════════════════════════════════════════════════════════════
    @Operation(summary = "Register a new user (DRIVER or MANAGER only)")
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Logout (client discards token)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    @Operation(summary = "Refresh JWT token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getToken()));
    }

    @Operation(summary = "Get current user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getUserByEmail(userDetails.getUsername()));
    }

    @Operation(summary = "Update user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @Operation(summary = "Change password",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        authService.changePassword(userId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @Operation(summary = "Deactivate account (soft delete)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        authService.deactivateAccount(userId);
        return ResponseEntity.ok("Account deactivated successfully");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — ADMIN USER MANAGEMENT ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Get all users — Admin only, optionally filtered by role",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(
            @RequestParam(required = false) String role) {
        Optional<User.Role> roleFilter = role != null && !role.isEmpty()
                ? Optional.of(User.Role.valueOf(role.toUpperCase()))
                : Optional.empty();
        return ResponseEntity.ok(authService.getAllUsers(roleFilter));
    }

    @Operation(
            summary = "Deactivate user account — Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<UserProfileResponse> deactivateUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(authService.deactivateUserAsAdmin(userId));
    }

    @Operation(
            summary = "Reactivate user account — Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<UserProfileResponse> reactivateUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(authService.reactivateUserAsAdmin(userId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — OTP ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    @Operation(summary = "Send OTP — for REGISTRATION or FORGOT_PASSWORD")
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(
            @Valid @RequestBody OtpSendRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @Operation(summary = "Verify OTP — shared for both REGISTRATION and FORGOT_PASSWORD")
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Operation(summary = "Forgot password — sends OTP to email (alias for send-otp)")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        // Convenience wrapper — delegates to sendOtp() with purpose fixed to FORGOT_PASSWORD
        // All rate-limiting, lockout and validation logic lives in sendOtp()
        OtpSendRequest otpRequest = new OtpSendRequest();
        otpRequest.setEmail(request.getEmail());
        otpRequest.setPurpose(com.parkease.auth.enums.OtpPurpose.FORGOT_PASSWORD);
        return ResponseEntity.ok(authService.sendOtp(otpRequest));
    }

    @Operation(summary = "Reset password — after OTP verified for FORGOT_PASSWORD")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════
    @Operation(summary = "Admin login — queries admins table only")
    @PostMapping("/admin/login")
    public ResponseEntity<AdminAuthResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }

    @Operation(
            summary = "Create new admin — Super Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/admin/create")
    public ResponseEntity<AdminProfileResponse> createAdmin(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AdminCreateRequest request) {
        String token = authHeader.substring(7);
        if (!jwtUtil.extractIsSuperAdmin(token)) {
            throw new AccessDeniedException("Super Admin privileges required to create admin");
        }
        UUID requesterId = UUID.fromString(jwtUtil.extractUserId(token));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createAdmin(request, requesterId));
    }

    @Operation(
            summary = "Soft delete admin — Super Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/admin/{adminId}")
    public ResponseEntity<String> deleteAdmin(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID adminId) {
        String token = authHeader.substring(7);
        if (!jwtUtil.extractIsSuperAdmin(token)) {
            throw new AccessDeniedException("Super Admin privileges required to delete admin");
        }
        UUID requesterId = UUID.fromString(jwtUtil.extractUserId(token));
        authService.deleteAdmin(adminId, requesterId);
        return ResponseEntity.ok("Admin deactivated successfully");
    }

    @Operation(
            summary = "Reactivate deactivated admin — Super Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/admin/{adminId}/reactivate")
    public ResponseEntity<AdminProfileResponse> reactivateAdmin(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID adminId) {
        String token = authHeader.substring(7);
        if (!jwtUtil.extractIsSuperAdmin(token)) {
            throw new AccessDeniedException("Super Admin privileges required to reactivate admin");
        }
        UUID requesterId = UUID.fromString(jwtUtil.extractUserId(token));
        return ResponseEntity.ok(authService.reactivateAdmin(adminId, requesterId));
    }

    @Operation(
            summary = "List all admins — Super Admin only",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin/all")
    public ResponseEntity<List<AdminProfileResponse>> getAllAdmins(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtUtil.extractIsSuperAdmin(token)) {
            throw new AccessDeniedException("Super Admin privileges required to list admins");
        }
        UUID requesterId = UUID.fromString(jwtUtil.extractUserId(token));
        return ResponseEntity.ok(authService.getAllAdmins(requesterId));
    }
}
