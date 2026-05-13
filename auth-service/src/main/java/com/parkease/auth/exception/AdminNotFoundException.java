package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class AdminNotFoundException extends AdminException {

    public AdminNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND");
    }
}
