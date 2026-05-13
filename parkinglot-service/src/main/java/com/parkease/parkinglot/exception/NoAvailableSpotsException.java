package com.parkease.parkinglot.exception;

/**
 * Thrown when parking lot has no available spots to decrement or is at
 * capacity. Maps to HTTP 409 Conflict.
 */
public class NoAvailableSpotsException extends RuntimeException {

    public NoAvailableSpotsException(String message) {
        super(message);
    }

    public NoAvailableSpotsException(String message, Throwable cause) {
        super(message, cause);
    }
}
