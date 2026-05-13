package com.parkease.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all auth-service exceptions. Every child exception
 * has a specific HTTP status and error code.
 */
public abstract class BaseAuthException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BaseAuthException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BaseAuthException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
