package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a spot is not in AVAILABLE status. Corresponds to HTTP
 * 409 Conflict.
 */
public class SpotUnavailableException extends BaseBookingException {

    public SpotUnavailableException(String message) {
        super(message, HttpStatus.CONFLICT, "SPOT_UNAVAILABLE");
    }

    public SpotUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "SPOT_UNAVAILABLE", cause);
    }
}
