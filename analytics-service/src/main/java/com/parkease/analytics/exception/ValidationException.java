package com.parkease.analytics.exception;

import java.util.List;
import java.util.Map;

/**
 * Thrown when business logic validation fails
 */
public class ValidationException extends RuntimeException {

    private final List<Map<String, String>> errors;

    public ValidationException(String message, List<Map<String, String>> errors) {
        super(message);
        this.errors = errors;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }
}
