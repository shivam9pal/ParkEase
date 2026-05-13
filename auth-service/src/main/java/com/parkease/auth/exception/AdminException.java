package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Admin-related exceptions hierarchy.
 */
public class AdminException extends BaseAuthException {

    public AdminException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
