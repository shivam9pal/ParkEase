package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpWrongAttemptsException extends OtpException {

    public OtpWrongAttemptsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS");
    }
}
