package com.parkease.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when dispatching a notification through multiple channels (EMAIL, SMS,
 * PUSH) encounters errors. This can be a PARTIAL failure (some channels
 * succeed, some fail). Returns 500 Internal Server Error or 207 Multi-Status
 * depending on severity.
 */
public class ChannelDispatchException extends ResponseStatusException {

    private final int successCount;
    private final int failureCount;

    public ChannelDispatchException(String message, int successCount, int failureCount) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public ChannelDispatchException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        this.successCount = 0;
        this.failureCount = 0;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public boolean isPartialFailure() {
        return successCount > 0 && failureCount > 0;
    }
}
