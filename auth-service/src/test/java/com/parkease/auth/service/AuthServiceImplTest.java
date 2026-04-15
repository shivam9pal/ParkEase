package com.parkease.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.parkease.auth.config.OtpConfig;
import com.parkease.auth.dto.LoginRequest;
import com.parkease.auth.dto.UpdateProfileRequest;
import com.parkease.auth.entity.User;
import com.parkease.auth.feign.MediaServiceClient;
import com.parkease.auth.repository.AdminRepository;
import com.parkease.auth.repository.OtpVerificationRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpConfig otpConfig;

    @Mock
    private MediaServiceClient mediaServiceClient;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .fullName("John Doe")
                .phone("9876543210")
                .passwordHash("hashedPassword123")
                .role(User.Role.DRIVER)
                .vehiclePlate("ABC123")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()));
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser.getEmail(), testUser.getRole().name(),
                testUser.getUserId().toString()))
                .thenReturn("jwtToken123");
        when(jwtUtil.getJwtExpiry()).thenReturn(3600L);

        // Act
        var response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwtToken123", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should fail login when user is deactivated")
    void testLoginFailWhenAccountDeactivated() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        testUser.setIsActive(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()));
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateTokenSuccess() {
        // Arrange
        String token = "validToken123";
        when(jwtUtil.extractEmail(token)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtUtil.isTokenValid(token, testUser.getEmail())).thenReturn(true);

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void testGetUserByEmailSuccess() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        // Act
        var response = authService.getUserByEmail(testUser.getEmail());

        // Assert
        assertNotNull(response);
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserByIdSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testUser));

        // Act
        var response = authService.getUserById(testUserId);

        // Assert
        assertNotNull(response);
        verify(userRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void testUpdateProfileSuccess() {
        // Arrange
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setPhone("9999999999");

        when(userRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        var response = authService.updateProfile(testUserId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should deactivate account successfully")
    void testDeactivateAccountSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        authService.deactivateAccount(testUserId);

        // Assert
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should get all active users")
    void testGetAllUsersSuccess() {
        // Arrange
        List<User> users = List.of(testUser);
        when(userRepository.findAllByIsActive(true))
                .thenReturn(users);

        // Act
        var response = authService.getAllUsers(Optional.empty());

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        verify(userRepository).findAllByIsActive(true);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() {
        // Arrange
        String oldToken = "oldToken123";
        String newToken = "newToken456";
        when(jwtUtil.isTokenExpired(oldToken)).thenReturn(false);
        when(jwtUtil.extractEmail(oldToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser.getEmail(), testUser.getRole().name(),
                testUser.getUserId().toString()))
                .thenReturn(newToken);
        when(jwtUtil.getJwtExpiry()).thenReturn(3600L);

        // Act
        var response = authService.refreshToken(oldToken);

        // Assert
        assertNotNull(response);
        assertEquals(newToken, response.getAccessToken());
    }

    @Test
    @DisplayName("Should fail refresh token when expired")
    void testRefreshTokenFailWhenExpired() {
        // Arrange
        String expiredToken = "expiredToken";
        when(jwtUtil.isTokenExpired(expiredToken)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.refreshToken(expiredToken));
    }

    @Test
    @DisplayName("Should deactivate user as admin successfully")
    void testDeactivateUserAsAdminSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        var response = authService.deactivateUserAsAdmin(testUserId);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendUserDeactivationEmail(testUser.getEmail(), testUser.getFullName());
    }

    @Test
    @DisplayName("Should reactivate user as admin successfully")
    void testReactivateUserAsAdminSuccess() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        var response = authService.reactivateUserAsAdmin(testUserId);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendUserReactivationEmail(testUser.getEmail(), testUser.getFullName());
    }
}
