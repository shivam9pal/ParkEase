package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

public class MediaServiceException extends ExternalServiceException {

    public MediaServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "MEDIA_SERVICE_ERROR");
    }

    public MediaServiceException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "MEDIA_SERVICE_ERROR");
    }
}
