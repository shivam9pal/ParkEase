package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when vehicle type is incompatible with spot type.
 * Corresponds to HTTP 400 Bad Request.
 */
public class SpotTypeIncompatibleException extends BaseBookingException {

    public SpotTypeIncompatibleException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "SPOT_TYPE_INCOMPATIBLE");
    }

    public SpotTypeIncompatibleException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "SPOT_TYPE_INCOMPATIBLE", cause);
    }
}
