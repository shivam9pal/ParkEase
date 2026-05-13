package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when booking type is invalid or unknown. Corresponds to HTTP
 * 400 Bad Request.
 */
public class InvalidBookingTypeException extends BaseBookingException {

    public InvalidBookingTypeException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_BOOKING_TYPE");
    }

    public InvalidBookingTypeException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_BOOKING_TYPE", cause);
    }
}
