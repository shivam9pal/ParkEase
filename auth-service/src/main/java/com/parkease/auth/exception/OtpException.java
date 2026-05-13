package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * OTP-related exceptions hierarchy.
 */
public class OtpException extends BaseAuthException {

    public OtpException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
