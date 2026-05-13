package com.parkease.vehicle.exception;

/**
 * Thrown when a vehicle resource is not found in the database. Maps to HTTP 404
 * Not Found.
 */
public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String message) {
        super(message);
    }

    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
