package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends UserManagementException {

    public EmailAlreadyRegisteredException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "EMAIL_EXISTS");
    }
}
