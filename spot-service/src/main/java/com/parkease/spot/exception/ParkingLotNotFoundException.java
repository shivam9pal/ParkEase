package com.parkease.spot.exception;

import java.util.UUID;

/**
 * Thrown when a parking lot with the given ID is not found. Mapped to 404
 * NOT_FOUND response with code LOT_NOT_FOUND.
 */
public class ParkingLotNotFoundException extends RuntimeException {

    private final UUID lotId;

    public ParkingLotNotFoundException(UUID lotId) {
        super("Parking lot not found with id: " + lotId);
        this.lotId = lotId;
    }

    public UUID getLotId() {
        return lotId;
    }
}
