package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when booking time is invalid (e.g., startTime in past,
 * endTime before startTime). Corresponds to HTTP 400 Bad Request.
 */
public class InvalidBookingTimeException extends BaseBookingException {

    public InvalidBookingTimeException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_BOOKING_TIME");
    }

    public InvalidBookingTimeException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_BOOKING_TIME", cause);
    }
}
