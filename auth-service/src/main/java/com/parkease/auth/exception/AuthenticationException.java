package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Authentication-related exceptions hierarchy.
 */
public class AuthenticationException extends BaseAuthException {

    public AuthenticationException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
