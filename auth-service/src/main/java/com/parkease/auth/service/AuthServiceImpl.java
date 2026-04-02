package com.parkease.auth.service;

import com.parkease.auth.dto.*;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : User.Role.DRIVER)
                .vehiclePlate(request.getVehiclePlate())
                .isActive(true)
                .build();
        User saved = userRepository.save(user);
        return mapToProfileResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId().toString()
        );

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiry())
                .user(mapToProfileResponse(user))
                .build();
    }

    @Override
    public void logout(String token) {
        // Stateless JWT: client discards token.
        // For production: add token to a Redis blacklist here.
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token);
            return userRepository.findByEmail(email)
                    .map(user -> jwtUtil.isTokenValid(token, email) && user.getIsActive())
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AuthResponse refreshToken(String token) {
        if (jwtUtil.isTokenExpired(token)) {
            throw new RuntimeException("Token is expired and cannot be refreshed");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId().toString()
        );
        return AuthResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiry())
                .user(mapToProfileResponse(user))
                .build();
    }

    @Override
    public UserProfileResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return mapToProfileResponse(user);
    }

    @Override
    public UserProfileResponse getUserById(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getVehiclePlate() != null) user.setVehiclePlate(request.getVehiclePlate());
        if (request.getProfilePicUrl() != null) user.setProfilePicUrl(request.getProfilePicUrl());
        return mapToProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
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
    }
}