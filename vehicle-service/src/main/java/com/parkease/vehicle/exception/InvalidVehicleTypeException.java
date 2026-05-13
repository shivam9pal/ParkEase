package com.parkease.vehicle.exception;

/**
 * Thrown when an invalid vehicle type is provided. Maps to HTTP 400 Bad
 * Request.
 */
public class InvalidVehicleTypeException extends RuntimeException {

    public InvalidVehicleTypeException(String message) {
        super(message);
    }

    public InvalidVehicleTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
