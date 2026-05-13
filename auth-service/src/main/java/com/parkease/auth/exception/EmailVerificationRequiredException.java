package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationRequiredException extends AuthenticationException {

    public EmailVerificationRequiredException(String message) {
        super(message, HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
    }
}
