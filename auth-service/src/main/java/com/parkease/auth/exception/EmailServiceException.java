package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailServiceException extends ExternalServiceException {

    public EmailServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "EMAIL_SERVICE_ERROR");
    }

    public EmailServiceException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "EMAIL_SERVICE_ERROR");
    }
}
