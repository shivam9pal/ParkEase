package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Password management-related exceptions hierarchy.
 */
public class PasswordException extends BaseAuthException {

    public PasswordException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
