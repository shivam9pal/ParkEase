package com.parkease.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a notification with the given ID is not found in the database.
 * Returns 404 Not Found.
 */
public class NotificationNotFoundException extends ResponseStatusException {

    public NotificationNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotificationNotFoundException(String notificationId, String message) {
        super(HttpStatus.NOT_FOUND, message + " — notificationId: " + notificationId);
    }
}
