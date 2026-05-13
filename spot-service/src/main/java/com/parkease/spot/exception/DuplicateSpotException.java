package com.parkease.spot.exception;

import java.util.UUID;

/**
 * Thrown when a spot number already exists within the same parking lot. Mapped
 * to 400 BAD_REQUEST response with code DUPLICATE_SPOT_NUMBER.
 */
public class DuplicateSpotException extends RuntimeException {

    private final UUID lotId;
    private final String spotNumber;

    public DuplicateSpotException(UUID lotId, String spotNumber) {
        super("Spot number '" + spotNumber + "' already exists in lot " + lotId);
        this.lotId = lotId;
        this.spotNumber = spotNumber;
    }

    public UUID getLotId() {
        return lotId;
    }

    public String getSpotNumber() {
        return spotNumber;
    }
}
