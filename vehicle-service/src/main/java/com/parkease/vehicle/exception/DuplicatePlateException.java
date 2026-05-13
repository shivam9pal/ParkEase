package com.parkease.vehicle.exception;

/**
 * Thrown when attempting to register a vehicle with a license plate that is
 * already registered to the same owner. Maps to HTTP 400 Bad Request.
 */
public class DuplicatePlateException extends RuntimeException {

    public DuplicatePlateException(String message) {
        super(message);
    }

    public DuplicatePlateException(String message, Throwable cause) {
        super(message, cause);
    }
}
