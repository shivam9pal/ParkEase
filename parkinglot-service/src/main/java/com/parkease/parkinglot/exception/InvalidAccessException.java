package com.parkease.parkinglot.exception;

/**
 * Thrown when user lacks permission to perform an action. Maps to HTTP 403
 * Forbidden.
 */
public class InvalidAccessException extends RuntimeException {

    public InvalidAccessException(String message) {
        super(message);
    }

    public InvalidAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
