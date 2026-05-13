package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when spot occupation fails during check-in. Corresponds to
 * HTTP 502 Bad Gateway.
 */
public class SpotOccupationException extends BaseBookingException {

    public SpotOccupationException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "SPOT_OCCUPATION_FAILED");
    }

    public SpotOccupationException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "SPOT_OCCUPATION_FAILED", cause);
    }
}
