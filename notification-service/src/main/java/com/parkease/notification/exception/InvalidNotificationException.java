package com.parkease.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a notification operation violates business rules. Examples:
 * unauthorized access, invalid state transitions, etc. Returns 403 Forbidden or
 * 400 Bad Request depending on context.
 */
public class InvalidNotificationException extends ResponseStatusException {

    public InvalidNotificationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public InvalidNotificationException(HttpStatus status, String message) {
        super(status, message);
    }

    public InvalidNotificationException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, message, cause);
    }
}
