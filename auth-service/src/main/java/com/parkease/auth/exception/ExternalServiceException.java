package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * External service-related exceptions hierarchy.
 */
public class ExternalServiceException extends BaseAuthException {

    public ExternalServiceException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }

    public ExternalServiceException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause, httpStatus, errorCode);
    }
}
