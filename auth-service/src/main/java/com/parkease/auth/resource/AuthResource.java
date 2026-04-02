package com.parkease.auth.resource;

import com.parkease.auth.dto.*;
import com.parkease.auth.entity.User;
import com.parkease.auth.security.JwtUtil;
import com.parkease.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management endpoints")
public class AuthResource {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // ── POST /api/v1/auth/register ────────────────────────────────────────────
    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────
    @Operation(summary = "Login and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────────────────
    @Operation(summary = "Logout (client discards token)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────────────────
    @Operation(summary = "Refresh JWT token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getToken()));
    }

    // ── GET /api/v1/auth/profile ──────────────────────────────────────────────
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getUserByEmail(userDetails.getUsername()));
    }

    // ── PUT /api/v1/auth/profile ──────────────────────────────────────────────
    @Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    // ── PUT /api/v1/auth/password ─────────────────────────────────────────────
    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        authService.changePassword(userId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    // ── DELETE /api/v1/auth/deactivate ────────────────────────────────────────
    @Operation(summary = "Deactivate account (soft delete)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(jwtUtil.extractUserId(authHeader.substring(7)));
        authService.deactivateAccount(userId);
        return ResponseEntity.ok("Account deactivated successfully");
    }
}