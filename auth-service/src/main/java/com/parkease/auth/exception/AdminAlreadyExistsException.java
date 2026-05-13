package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class AdminAlreadyExistsException extends AdminException {

    public AdminAlreadyExistsException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "ADMIN_EXISTS");
    }
}
