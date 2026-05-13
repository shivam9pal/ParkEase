package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when parking-lot-service is unavailable or returns an error.
 * Corresponds to HTTP 502 Bad Gateway.
 */
public class LotServiceUnavailableException extends BaseBookingException {

    public LotServiceUnavailableException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "LOT_SERVICE_UNAVAILABLE");
    }

    public LotServiceUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "LOT_SERVICE_UNAVAILABLE", cause);
    }
}
