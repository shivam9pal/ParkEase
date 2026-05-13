package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is not authorized to perform an operation on a
 * booking. For example: attempting to check out another user's booking.
 * Corresponds to HTTP 403 Forbidden.
 */
public class BookingNotOwnedException extends BaseBookingException {

    public BookingNotOwnedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "BOOKING_NOT_OWNED");
    }

    public BookingNotOwnedException(String message, Throwable cause) {
        super(message, HttpStatus.FORBIDDEN, "BOOKING_NOT_OWNED", cause);
    }
}
