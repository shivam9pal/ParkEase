package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidAdminPasswordException extends AdminException {

    public InvalidAdminPasswordException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_ADMIN_PASSWORD");
    }
}
