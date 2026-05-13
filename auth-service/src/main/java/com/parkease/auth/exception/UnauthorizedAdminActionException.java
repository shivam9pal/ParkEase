package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAdminActionException extends AdminException {

    public UnauthorizedAdminActionException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
