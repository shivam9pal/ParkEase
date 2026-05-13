package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the spot-service is unavailable or returns an error.
 * Corresponds to HTTP 502 Bad Gateway.
 */
public class SpotServiceUnavailableException extends BaseBookingException {

    public SpotServiceUnavailableException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "SPOT_SERVICE_UNAVAILABLE");
    }

    public SpotServiceUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "SPOT_SERVICE_UNAVAILABLE", cause);
    }
}
