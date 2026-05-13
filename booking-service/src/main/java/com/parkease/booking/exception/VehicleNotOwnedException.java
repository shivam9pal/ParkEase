package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a vehicle does not belong to the requesting user.
 * Corresponds to HTTP 403 Forbidden.
 */
public class VehicleNotOwnedException extends BaseBookingException {

    public VehicleNotOwnedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "VEHICLE_NOT_OWNED");
    }

    public VehicleNotOwnedException(String message, Throwable cause) {
        super(message, HttpStatus.FORBIDDEN, "VEHICLE_NOT_OWNED", cause);
    }
}
