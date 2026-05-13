package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested spot is not found. Corresponds to HTTP 404
 * Not Found.
 */
public class SpotNotFoundException extends BaseBookingException {

    public SpotNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "SPOT_NOT_FOUND");
    }

    public SpotNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, "SPOT_NOT_FOUND", cause);
    }
}
