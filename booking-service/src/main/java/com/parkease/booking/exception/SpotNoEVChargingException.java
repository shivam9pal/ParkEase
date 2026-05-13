package com.parkease.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an EV vehicle requires a charging spot that is not
 * available. Corresponds to HTTP 400 Bad Request.
 */
public class SpotNoEVChargingException extends BaseBookingException {

    public SpotNoEVChargingException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "SPOT_NO_EV_CHARGING");
    }

    public SpotNoEVChargingException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, "SPOT_NO_EV_CHARGING", cause);
    }
}
