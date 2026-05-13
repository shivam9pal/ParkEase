package com.parkease.auth.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkease.auth.config.OtpConfig;
import com.parkease.auth.exception.AccountDeactivatedException;
import com.parkease.auth.exception.AdminAlreadyExistsException;
import com.parkease.auth.exception.AdminDeactivatedException;
import com.parkease.auth.exception.AdminNotFoundException;
import com.parkease.auth.exception.EmailAlreadyRegisteredException;
import com.parkease.auth.exception.EmailServiceException;
import com.parkease.auth.exception.EmailVerificationRequiredException;
import com.parkease.auth.exception.IncorrectPasswordException;
import com.parkease.auth.exception.InvalidAdminPasswordException;
import com.parkease.auth.exception.MediaServiceException;
import com.parkease.auth.exception.OtpAlreadyUsedException;
import com.parkease.auth.exception.OtpCooldownException;
import com.parkease.auth.exception.OtpException;
import com.parkease.auth.exception.OtpNotFoundException;
import com.parkease.auth.exception.OtpRateLimitException;
import com.parkease.auth.exception.OtpWrongAttemptsException;
import com.parkease.auth.exception.OtpExpiredException;
import com.parkease.auth.exception.PasswordResetNotAvailableException;
import com.parkease.auth.exception.S3UploadException;
import com.parkease.auth.exception.TokenExpiredException;
import com.parkease.auth.exception.UnauthorizedAdminActionException;
import com.parkease.auth.exception.UserAlreadyActiveException;
import com.parkease.auth.exception.UserAlreadyDeactivatedException;
import com.parkease.auth.exception.UserNotFoundException;
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
import com.parkease.auth.entity.Admin;
import com.parkease.auth.entity.OtpVerification;
import com.parkease.auth.entity.User;
import com.parkease.auth.enums.OtpPurpose;
import com.parkease.auth.feign.MediaServiceClient;
import com.parkease.auth.repository.AdminRepository;
import com.parkease.auth.repository.OtpVerificationRepository;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpVerificationRepository otpVerificationRepository;
    private final AdminRepository adminRepository;
    private final EmailService emailService;
    private final OtpConfig otpConfig;
    private final MediaServiceClient mediaServiceClient;


    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {

        // ── OTP Guard (NEW — added at very start) ─────────────────────────────
        OtpVerification otpRecord = otpVerificationRepository
                .findByEmailAndPurpose(request.getEmail(), OtpPurpose.REGISTRATION)
                .orElseThrow(() -> new OtpException(
                "Please verify your email with OTP before registering.", HttpStatus.BAD_REQUEST, "OTP_NOT_VERIFIED"));

        if (!otpRecord.isVerified()) {
            throw new EmailVerificationRequiredException(
                    "Email not verified. Please complete OTP verification first.");
        }

        if (otpRecord.isUsed()) {
            throw new OtpAlreadyUsedException(
                    "OTP session expired. Please request a new OTP.");
        }
        // ── End OTP Guard ──────────────────────────────────────────────────────

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email already registered: " + request.getEmail());
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

        // Consume the OTP — prevents reuse for another registration
        otpRecord.setUsed(true);
        otpVerificationRepository.save(otpRecord);

        return mapToProfileResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getIsActive()) {
            throw new AccountDeactivatedException("Account is deactivated");
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
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getVehiclePlate() != null) {
            user.setVehiclePlate(request.getVehiclePlate());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }
        return mapToProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAndUpdateProfilePicture(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        log.info("=== UPLOAD PROFILE PICTURE START ===");
        log.info("UserId: {}, FileName: {}, FileSize: {}, ContentType: {}",
                userId, file.getOriginalFilename(), file.getSize(), file.getContentType());

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        log.info("User found: {}", user.getEmail());

        try {
            // Upload to Media Service via Feign client
            log.info("Calling Media Service Feign client...");
            var apiResponse = mediaServiceClient.uploadFile(
                    file,
                    userId,
                    "PROFILE_PIC",
                    userId.toString(),
                    "USER"
            );
            log.info("Media Service response received: {}", apiResponse);

            // Extract FileUploadResponse from ApiResponse wrapper
            var fileUploadResponse = apiResponse.getData();
            if (fileUploadResponse == null) {
                log.error("FileUploadResponse is NULL from Media Service");
                throw new RuntimeException("Failed to get upload response from media service");
            }
            log.info("FileUploadResponse extracted: uploadId={}, fileUrl={}",
                    fileUploadResponse.getUploadId(), fileUploadResponse.getFileUrl());

            // Extract S3 URL from response
            String s3Url = fileUploadResponse.getFileUrl();
            if (s3Url == null || s3Url.isEmpty()) {
                log.error("S3 URL is NULL or empty from Media Service");
                throw new RuntimeException("S3 URL not returned from media service");
            }
            log.info("S3 URL extracted: {}", s3Url);

            // Update user with S3 URL
            user.setProfilePicUrl(s3Url);
            User savedUser = userRepository.save(user);
            log.info("User saved with profilePicUrl: {}", savedUser.getProfilePicUrl());

            UserProfileResponse response = mapToProfileResponse(savedUser);
            log.info("=== UPLOAD PROFILE PICTURE SUCCESS ===");
            return response;

        } catch (Exception e) {
            log.error("ERROR in uploadAndUpdateProfilePicture: {}", e.getMessage(), e);
            throw new S3UploadException("File upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — ADMIN USER MANAGEMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public List<UserProfileResponse> getAllUsers(Optional<User.Role> roleFilter) {
        List<User> users;

        // Filter by role if provided, otherwise get all users
        if (roleFilter.isPresent()) {
            users = userRepository.findAllByRoleAndIsActive(roleFilter.get(), true);
        } else {
            users = userRepository.findAllByIsActive(true);
        }

        return users.stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserDetailDto getUserDetailById(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return mapToUserDetailDto(user);
    }

    @Override
    public List<UserDetailDto> getUserDetailsByRole(User.Role role) {
        List<User> users = userRepository.findAllByRoleAndIsActive(role, true);
        return users.stream()
                .map(this::mapToUserDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfileResponse deactivateUserAsAdmin(UUID userId) {
        // Step 1: User must exist
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Step 2: Already inactive?
        if (!user.getIsActive()) {
            throw new UserAlreadyDeactivatedException("User is already deactivated");
        }

        // Step 3: Deactivate (soft delete)
        user.setIsActive(false);
        User saved = userRepository.save(user);

        // Step 4: Send deactivation email notification
        try {
            emailService.sendUserDeactivationEmail(saved.getEmail(), saved.getFullName());
        } catch (Exception e) {
            // Log error but don't block the transaction — user is already deactivated
            log.warn("Failed to send deactivation email to {}: {}", saved.getEmail(), e.getMessage());
        }

        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse reactivateUserAsAdmin(UUID userId) {
        // Step 1: User must exist
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Step 2: Already active?
        if (user.getIsActive()) {
            throw new UserAlreadyActiveException("User is already active");
        }

        // Step 3: Reactivate
        user.setIsActive(true);
        User saved = userRepository.save(user);

        // Step 4: Send reactivation email notification
        try {
            emailService.sendUserReactivationEmail(saved.getEmail(), saved.getFullName());
        } catch (Exception e) {
            // Log error but don't block the transaction — user is already reactivated
            log.warn("Failed to send reactivation email to {}: {}", saved.getEmail(), e.getMessage());
        }

        return mapToProfileResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — OTP METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public String sendOtp(OtpSendRequest request) {
        String email = request.getEmail();
        OtpPurpose purpose = request.getPurpose();

        // Step 1: FORGOT_PASSWORD — validate user exists, is active, and has a password
        if (purpose == OtpPurpose.FORGOT_PASSWORD) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(
                    "No account found with this email"));

            if (!user.getIsActive()) {
                throw new AccountDeactivatedException(
                        "Account is deactivated. Contact support.");
            }

            if (user.getPasswordHash() == null) {
                throw new PasswordResetNotAvailableException(
                        "This account uses Google/GitHub login. Password reset is not available.");
            }
        }

        // Step 2: Look up existing OTP record for (email + purpose)
        Optional<OtpVerification> existingOpt
                = otpVerificationRepository.findByEmailAndPurpose(email, purpose);

        LocalDateTime now = LocalDateTime.now();

        // Step 3: No record — create fresh and send
        if (existingOpt.isEmpty()) {
            String code = generateOtpCode();
            OtpVerification otp = OtpVerification.builder()
                    .email(email)
                    .purpose(purpose)
                    .otpCode(code)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(otpConfig.getExpiryMinutes()))
                    .verified(false)
                    .used(false)
                    .attemptCount(1)
                    .wrongAttemptCount(0)
                    .lockedUntil(null)
                    .build();
            otpVerificationRepository.save(otp);

            try {
                emailService.sendOtpEmail(email, code, purpose == OtpPurpose.REGISTRATION);
                log.info("OTP sent successfully for {} ({})", email, purpose);
            } catch (EmailServiceException e) {
                // ── Rollback: Delete OTP if email sending fails ─────────────────────
                log.warn("Email delivery failed for {}. Rolling back OTP creation.", email, e);
                otpVerificationRepository.delete(otp);
                throw e;  // Re-throw to let GlobalExceptionHandler process it
            }
            return "OTP sent successfully to " + email;
        }

        // Step 4: Record exists — run checks IN EXACT ORDER
        OtpVerification otp = existingOpt.get();

        // CHECK A: Active lockout?
        if (otp.getLockedUntil() != null && otp.getLockedUntil().isAfter(now)) {
            throw new OtpRateLimitException(
                    "Too many OTP requests. Try again after "
                    + formatLockoutTime(otp.getLockedUntil()));
        }

        // CHECK B: Lockout expired — lazy reset (do NOT save yet)
        if (otp.getLockedUntil() != null && !otp.getLockedUntil().isAfter(now)) {
            otp.setAttemptCount(0);
            otp.setWrongAttemptCount(0);
            otp.setLockedUntil(null);
        }

        // CHECK C: Cooldown window still active?
        LocalDateTime cooldownEnd
                = otp.getCreatedAt().plusSeconds(otpConfig.getResendWindowSeconds());
        if (now.isBefore(cooldownEnd)) {
            long secondsLeft = java.time.Duration.between(now, cooldownEnd).getSeconds();
            throw new OtpCooldownException(
                    "Please wait " + secondsLeft + " seconds before requesting a new OTP");
        }

        // CHECK D: Attempt limit hit? → lock and throw
        if (otp.getAttemptCount() >= otpConfig.getMaxAttempts()) {
            LocalDateTime lockUntil = now.plusHours(otpConfig.getLockoutDurationHours());
            otp.setLockedUntil(lockUntil);
            otpVerificationRepository.save(otp);
            throw new OtpRateLimitException(
                    "Too many OTP requests. Try again after "
                    + formatLockoutTime(lockUntil));
        }

        // ALL CHECKS PASSED — refresh OTP record and send
        String newCode = generateOtpCode();
        otp.setOtpCode(newCode);
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusMinutes(otpConfig.getExpiryMinutes()));
        otp.setVerified(false);
        otp.setUsed(false);
        otp.setWrongAttemptCount(0);                          // always reset on new OTP
        otp.setAttemptCount(otp.getAttemptCount() + 1);
        otpVerificationRepository.save(otp);

        try {
            emailService.sendOtpEmail(email, newCode, purpose == OtpPurpose.REGISTRATION);
            log.info("OTP resent successfully for {} ({})", email, purpose);
        } catch (EmailServiceException e) {
            // ── Rollback: Restore previous OTP attempt count if email fails ────────
            log.warn("Email delivery failed during OTP resend for {}. Rolling back attempt count.", email, e);
            otp.setAttemptCount(otp.getAttemptCount() - 1);
            otpVerificationRepository.save(otp);
            throw e;  // Re-throw to let GlobalExceptionHandler process it
        }
        return "OTP sent successfully to " + email;
    }

    @Override
    @Transactional
    public String verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail();
        OtpPurpose purpose = request.getPurpose();

        // Step 1: Record must exist
        OtpVerification otp = otpVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new OtpNotFoundException(
                "OTP not found. Please request a new one."));

        // Step 2: Already consumed?
        if (otp.isUsed()) {
            throw new OtpAlreadyUsedException("OTP already used. Please request a new one.");
        }

        // Step 3: Expired?
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }

        // Step 4: Wrong code?
        if (!otp.getOtpCode().equals(request.getOtp())) {
            otp.setWrongAttemptCount(otp.getWrongAttemptCount() + 1);
            otpVerificationRepository.save(otp);

            if (otp.getWrongAttemptCount() >= otpConfig.getMaxVerifyAttempts()) {
                // Kill this OTP — user must request a new one
                otp.setUsed(true);
                otpVerificationRepository.save(otp);
                throw new OtpWrongAttemptsException(
                        "Too many wrong attempts. Please request a new OTP.");
            }

            int remaining = otpConfig.getMaxVerifyAttempts() - otp.getWrongAttemptCount();
            throw new OtpException(
                    "Invalid OTP. " + remaining + " attempt(s) remaining.", HttpStatus.BAD_REQUEST, "INVALID_OTP");
        }

        // Step 5: Correct — mark verified
        otp.setVerified(true);
        otpVerificationRepository.save(otp);
        return "Email verified successfully";
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();

        // Step 1: OTP record must exist for FORGOT_PASSWORD
        OtpVerification otpRecord = otpVerificationRepository
                .findByEmailAndPurpose(email, OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow(() -> new OtpException(
                "Please verify your email with OTP first.", HttpStatus.BAD_REQUEST, "OTP_NOT_VERIFIED"));

        // Step 2: Must be verified
        if (!otpRecord.isVerified()) {
            throw new EmailVerificationRequiredException(
                    "Email not verified. Please complete OTP verification first.");
        }

        // Step 3: Must not be already consumed
        if (otpRecord.isUsed()) {
            throw new OtpAlreadyUsedException(
                    "OTP session expired. Please request a new OTP.");
        }

        // Step 4: Update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Step 5: Consume OTP — prevents reuse
        otpRecord.setUsed(true);
        otpVerificationRepository.save(otpRecord);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW — ADMIN METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    public AdminAuthResponse adminLogin(AdminLoginRequest request) {

        // Step 1: Email must exist in admins table
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AdminNotFoundException("Admin Email not found"));

        // Step 2: Account must be active
        if (!admin.isActive()) {
            throw new AdminDeactivatedException("Admin account is deactivated");
        }

        // Step 3: Password must match
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new InvalidAdminPasswordException("Invalid Admin password");
        }

        // Step 4: Issue admin JWT with isSuperAdmin claim
        String token = jwtUtil.generateAdminToken(
                admin.getEmail(),
                admin.getAdminId().toString(),
                admin.getRole(),
                admin.isSuperAdmin()
        );

        return AdminAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiry())
                .admin(mapToAdminProfileResponse(admin))
                .build();
    }

    @Override
    @Transactional
    public AdminProfileResponse createAdmin(AdminCreateRequest request, UUID requesterId) {

        // Step 1: Requester must be Super Admin
        Admin requester = adminRepository.findByAdminId(requesterId)
                .orElseThrow(() -> new UnauthorizedAdminActionException(
                "Only Super Admin can create new admins"));

        if (!requester.isSuperAdmin()) {
            throw new UnauthorizedAdminActionException("Only Super Admin can create new admins");
        }

        // Step 2: Email must not already exist in admins table
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new AdminAlreadyExistsException("Admin with this email already exists");
        }

        // Step 3: Create — isSuperAdmin is ALWAYS false for created admins
        Admin newAdmin = Admin.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .isActive(true)
                .isSuperAdmin(false)
                .createdAt(LocalDateTime.now())
                .build();

        return mapToAdminProfileResponse(adminRepository.save(newAdmin));
    }

    @Override
    @Transactional
    public void deleteAdmin(UUID targetAdminId, UUID requesterId) {

        // Step 1: Requester must be Super Admin
        Admin requester = adminRepository.findByAdminId(requesterId)
                .orElseThrow(() -> new RuntimeException(
                "Only Super Admin can delete admins"));

        if (!requester.isSuperAdmin()) {
            throw new RuntimeException("Only Super Admin can delete admins");
        }

        // Step 2: Target must exist
        Admin target = adminRepository.findByAdminId(targetAdminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Step 3: Super Admin is permanently undeletable
        if (target.isSuperAdmin()) {
            throw new RuntimeException("Super Admin cannot be deleted");
        }

        // Step 4: Soft delete — keep audit record
        target.setActive(false);
        adminRepository.save(target);
    }

    @Override
    @Transactional
    public AdminProfileResponse reactivateAdmin(UUID targetAdminId, UUID requesterId) {

        // Step 1: Requester must be Super Admin
        Admin requester = adminRepository.findByAdminId(requesterId)
                .orElseThrow(() -> new RuntimeException(
                "Only Super Admin can reactivate admins"));

        if (!requester.isSuperAdmin()) {
            throw new RuntimeException("Only Super Admin can reactivate admins");
        }

        // Step 2: Target must exist
        Admin target = adminRepository.findByAdminId(targetAdminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Step 3: Super Admin cannot be reactivated (they can't be deactivated either)
        if (target.isSuperAdmin()) {
            throw new RuntimeException("Super Admin cannot be modified");
        }

        // Step 4: Check if already active
        if (target.isActive()) {
            throw new RuntimeException("Admin is already active");
        }

        // Step 5: Reactivate
        target.setActive(true);
        Admin reactivated = adminRepository.save(target);
        return mapToAdminProfileResponse(reactivated);
    }

    @Override
    public List<AdminProfileResponse> getAllAdmins(UUID requesterId) {

        // Step 1: Requester must be Super Admin
        Admin requester = adminRepository.findByAdminId(requesterId)
                .orElseThrow(() -> new RuntimeException(
                "Only Super Admin can view all admins"));

        if (!requester.isSuperAdmin()) {
            throw new RuntimeException("Only Super Admin can view all admins");
        }

        // Step 2: Return all admins newest first
        return adminRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToAdminProfileResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ═══════════════════════════════════════════════════════════════════════════
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

    private UserDetailDto mapToUserDetailDto(User user) {
        return new UserDetailDto(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive()
        );
    }

    private AdminProfileResponse mapToAdminProfileResponse(Admin admin) {
        return AdminProfileResponse.builder()
                .adminId(admin.getAdminId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .isActive(admin.isActive())
                .isSuperAdmin(admin.isSuperAdmin())
                .createdAt(admin.getCreatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    // Generates a secure random 6-digit OTP — always exactly 6 digits (zero-padded)
    private String generateOtpCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    // Formats lockout time for user-facing error messages: "14:30, 08 Apr"
    private String formatLockoutTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm, dd MMM"));
    }
}
