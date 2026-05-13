package com.parkease.spot.exception;

import java.util.UUID;

/**
 * Thrown when a parking spot with the given ID is not found in the database.
 * Mapped to 404 NOT_FOUND response.
 */
public class SpotNotFoundException extends RuntimeException {

    private final UUID spotId;

    public SpotNotFoundException(UUID spotId) {
        super("Spot not found with id: " + spotId);
        this.spotId = spotId;
    }

    public UUID getSpotId() {
        return spotId;
    }
}
