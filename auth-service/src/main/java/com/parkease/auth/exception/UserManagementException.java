package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * User management-related exceptions hierarchy.
 */
public class UserManagementException extends BaseAuthException {

    public UserManagementException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
