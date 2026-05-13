package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a booking is in an invalid state for the requested
 * operation. For example: attempting to check-in an already active booking.
 * Corresponds to HTTP 409 Conflict.
 */
public class BookingStateException extends BaseBookingException {

    public BookingStateException(String message) {
        super(message, HttpStatus.CONFLICT, "BOOKING_STATE_INVALID");
    }

    public BookingStateException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "BOOKING_STATE_INVALID", cause);
    }
}
