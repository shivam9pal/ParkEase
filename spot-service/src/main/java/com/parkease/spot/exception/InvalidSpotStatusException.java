package com.parkease.spot.exception;

import java.util.UUID;
import com.parkease.spot.entity.SpotStatus;

/**
 * Thrown when an operation cannot be performed on a spot due to its current
 * status. Examples: Cannot reserve an OCCUPIED spot, cannot occupy a
 * MAINTENANCE spot. Mapped to 409 CONFLICT response with code
 * INVALID_STATUS_TRANSITION.
 */
public class InvalidSpotStatusException extends RuntimeException {

    private final UUID spotId;
    private final SpotStatus currentStatus;
    private final String attemptedOperation;

    public InvalidSpotStatusException(UUID spotId, SpotStatus currentStatus, String attemptedOperation) {
        super("Cannot " + attemptedOperation + " spot in " + currentStatus + " status");
        this.spotId = spotId;
        this.currentStatus = currentStatus;
        this.attemptedOperation = attemptedOperation;
    }

    public UUID getSpotId() {
        return spotId;
    }

    public SpotStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedOperation() {
        return attemptedOperation;
    }
}
