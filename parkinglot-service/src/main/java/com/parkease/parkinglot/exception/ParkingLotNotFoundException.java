package com.parkease.parkinglot.exception;

/**
 * Thrown when a parking lot with the given ID does not exist. Maps to HTTP 404
 * Not Found.
 */
public class ParkingLotNotFoundException extends RuntimeException {

    public ParkingLotNotFoundException(String message) {
        super(message);
    }

    public ParkingLotNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
