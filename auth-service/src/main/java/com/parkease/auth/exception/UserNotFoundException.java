package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserManagementException {

    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
