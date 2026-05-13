package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when saving a booking fails. Corresponds to HTTP 500
 * Internal Server Error.
 */
public class BookingSaveException extends BaseBookingException {

    public BookingSaveException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "BOOKING_SAVE_FAILED");
    }

    public BookingSaveException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "BOOKING_SAVE_FAILED", cause);
    }
}
