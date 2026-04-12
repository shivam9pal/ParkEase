package com.parkease.auth.repository;

import com.parkease.auth.entity.OtpVerification;
import com.parkease.auth.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    // Primary lookup — every OTP operation uses (email + purpose) as the key
    Optional<OtpVerification> findByEmailAndPurpose(String email, OtpPurpose purpose);

    // Used at end of register() and resetPassword() to clean up after consumption
    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);

    // Used to check if a verified-but-unused OTP exists before register/resetPassword
    boolean existsByEmailAndPurposeAndVerifiedTrueAndUsedFalse(String email, OtpPurpose purpose);
}