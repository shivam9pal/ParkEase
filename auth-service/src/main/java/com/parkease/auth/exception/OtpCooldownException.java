package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpCooldownException extends OtpException {

    public OtpCooldownException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN_ACTIVE");
    }
}
