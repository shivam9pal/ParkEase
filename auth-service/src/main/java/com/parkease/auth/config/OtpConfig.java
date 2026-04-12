package com.parkease.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
@Data
public class OtpConfig {

    private int expiryMinutes;
    private int resendWindowSeconds;
    private int maxAttempts;
    private int lockoutDurationHours;
    private int maxVerifyAttempts;
}