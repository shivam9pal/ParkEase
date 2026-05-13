package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a vehicle is deactivated and cannot be used for
 * booking. Corresponds to HTTP 400 Bad Request.
 */
public class VehicleDeactivatedException extends BaseBookingException {

    public VehicleDeactivatedException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VEHICLE_DEACTIVATED");
    }

    public VehicleDeactivatedException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "VEHICLE_DEACTIVATED", cause);
    }
}
