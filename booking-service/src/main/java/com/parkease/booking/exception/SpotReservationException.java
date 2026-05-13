package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when spot reservation or occupation fails. Corresponds to
 * HTTP 409 Conflict.
 */
public class SpotReservationException extends BaseBookingException {

    public SpotReservationException(String message) {
        super(message, HttpStatus.CONFLICT, "SPOT_RESERVATION_FAILED");
    }

    public SpotReservationException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "SPOT_RESERVATION_FAILED", cause);
    }
}
