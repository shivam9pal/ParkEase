package com.parkease.auth.security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.config.OAuth2Properties;
import com.parkease.auth.dto.UserProfileResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called by Spring Security after OAuth2 login succeeds. Generates a JWT and
 * redirects to frontend callback URL with auth data in query params.
 *
 * Frontend flow: 1. User clicks "Login with Google" → GET
 * /oauth2/authorization/google 2. Google redirects back →
 * /login/oauth2/code/google 3. Spring Security calls this handler 4. We
 * redirect to frontend callback URL with token and user data 5. Frontend
 * callback page extract data and postMessage to main window
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final OAuth2Properties oAuth2Properties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // Generate JWT the same way as email/password login
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId().toString()
        );

        // Build user profile response
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .vehiclePlate(user.getVehiclePlate())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .profilePicUrl(user.getProfilePicUrl())
                .build();

        // Convert user profile to JSON
        String userJson = objectMapper.writeValueAsString(userProfile);

        // Get frontend callback URL from properties
        String frontendCallbackUrl = oAuth2Properties.getFrontend().getCallbackUrl();

        // Build redirect URL with encoded parameters
        String redirectUrl = String.format(
                "%s?token=%s&user=%s",
                frontendCallbackUrl,
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(userJson, StandardCharsets.UTF_8)
        );

        log.info("OAuth2 success. Redirecting to: {}", frontendCallbackUrl);
        response.sendRedirect(redirectUrl);
    }
}
