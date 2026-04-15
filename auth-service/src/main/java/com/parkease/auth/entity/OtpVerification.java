package com.parkease.auth.entity;

import com.parkease.auth.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "otp_verifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_email_purpose",
                columnNames = {"email", "purpose"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(nullable = false, length = 6)
    private String otpCode;

    // Timestamp of last OTP generation — used for cooldown window check
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // createdAt + otp.expiry-minutes
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // true after /verify-otp succeeds
    @Column(nullable = false)
    private boolean verified = false;

    // true after /register or /reset-password consumes the OTP
    @Column(nullable = false)
    private boolean used = false;

    // counts /send-otp calls — triggers lockout when >= otp.max-attempts
    @Column(nullable = false)
    private int attemptCount = 0;

    // counts wrong OTP entries — reset to 0 on every new OTP generation
    @Column(nullable = false)
    private int wrongAttemptCount = 0;

    // null = not locked | set when attemptCount hits max
    @Column(nullable = true)
    private LocalDateTime lockedUntil;
}