package com.parkease.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when fetching user details from auth-service fails. This is a
 * SERVICE_UNAVAILABLE error since it's an external service dependency. Returns
 * 503 Service Unavailable.
 */
public class UserFetchException extends ResponseStatusException {

    public UserFetchException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public UserFetchException(String userId, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message + " — userId: " + userId);
    }

    public UserFetchException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
