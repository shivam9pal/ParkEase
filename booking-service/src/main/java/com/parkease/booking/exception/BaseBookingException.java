package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all booking-service domain exceptions. Each subclass
 * specifies its HTTP status and error code.
 */
public abstract class BaseBookingException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BaseBookingException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BaseBookingException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
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
