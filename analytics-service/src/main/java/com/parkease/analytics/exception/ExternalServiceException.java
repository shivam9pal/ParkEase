package com.parkease.analytics.exception;

/**
 * Thrown when an external service (e.g., payment-service, booking-service) is
 * temporarily unavailable
 */
public class ExternalServiceException extends RuntimeException {

    private final int httpStatus;
    private final String serviceName;

    public ExternalServiceException(String message, String serviceName, int httpStatus) {
        super(message);
        this.serviceName = serviceName;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getServiceName() {
        return serviceName;
    }
}
