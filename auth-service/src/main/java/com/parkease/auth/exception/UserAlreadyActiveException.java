package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyActiveException extends UserManagementException {

    public UserAlreadyActiveException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "USER_ALREADY_ACTIVE");
    }
}
