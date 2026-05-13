package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyDeactivatedException extends UserManagementException {

    public UserAlreadyDeactivatedException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "USER_ALREADY_INACTIVE");
    }
}
