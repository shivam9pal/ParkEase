package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpRateLimitException extends OtpException {

    public OtpRateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED");
    }
}
