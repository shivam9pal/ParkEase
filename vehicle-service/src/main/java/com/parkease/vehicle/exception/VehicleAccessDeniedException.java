package com.parkease.vehicle.exception;

/**
 * Thrown when a user attempts to access or modify a vehicle they don't own.
 * Maps to HTTP 403 Forbidden.
 */
public class VehicleAccessDeniedException extends RuntimeException {

    public VehicleAccessDeniedException(String message) {
        super(message);
    }

    public VehicleAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
