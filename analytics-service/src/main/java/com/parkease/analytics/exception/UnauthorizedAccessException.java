package com.parkease.analytics.exception;

import java.util.UUID;

/**
 * Thrown when a manager/user tries to access a resource they don't own
 */
public class UnauthorizedAccessException extends RuntimeException {

    private final UUID userId;
    private final UUID resourceId;
    private final String resourceType;

    public UnauthorizedAccessException(String message, UUID userId, UUID resourceId, String resourceType) {
        super(message);
        this.userId = userId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }
}
