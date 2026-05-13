package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class PasswordResetNotAvailableException extends PasswordException {

    public PasswordResetNotAvailableException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "PASSWORD_RESET_UNAVAILABLE");
    }
}
