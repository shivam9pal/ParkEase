package com.parkease.auth.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.dto.AuthResponse;
import com.parkease.auth.dto.UserProfileResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called by Spring Security after OAuth2 login succeeds.
 * Generates a JWT and writes the AuthResponse JSON directly
 * to the HTTP response (pure REST — no redirect needed).
 *
 * Frontend flow:
 *   1. User clicks "Login with Google" → GET /oauth2/authorization/google
 *   2. Google redirects back → /login/oauth2/code/google
 *   3. Spring Security calls this handler
 *   4. We return JSON with JWT — frontend stores it and proceeds
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // Generate JWT exactly the same way as email/password login
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId().toString()
        );

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiry())
                .user(UserProfileResponse.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .vehiclePlate(user.getVehiclePlate())
                        .isActive(user.getIsActive())
                        .createdAt(user.getCreatedAt())
                        .profilePicUrl(user.getProfilePicUrl())
                        .build())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), authResponse);
    }
}
