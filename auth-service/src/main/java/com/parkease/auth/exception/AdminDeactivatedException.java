package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class AdminDeactivatedException extends AdminException {

    public AdminDeactivatedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "ADMIN_INACTIVE");
    }
}
