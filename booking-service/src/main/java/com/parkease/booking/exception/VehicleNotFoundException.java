package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested vehicle is not found. Corresponds to HTTP
 * 404 Not Found.
 */
public class VehicleNotFoundException extends BaseBookingException {

    public VehicleNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND");
    }

    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND", cause);
    }
}
