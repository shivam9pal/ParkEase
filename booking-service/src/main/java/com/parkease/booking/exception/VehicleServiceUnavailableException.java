package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the vehicle-service is unavailable or returns an error.
 * Corresponds to HTTP 502 Bad Gateway.
 */
public class VehicleServiceUnavailableException extends BaseBookingException {

    public VehicleServiceUnavailableException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "VEHICLE_SERVICE_UNAVAILABLE");
    }

    public VehicleServiceUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, "VEHICLE_SERVICE_UNAVAILABLE", cause);
    }
}
