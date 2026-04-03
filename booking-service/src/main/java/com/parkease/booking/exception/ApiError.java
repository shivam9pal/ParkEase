package com.parkease.booking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified error response structure returned by GlobalExceptionHandler.
 * Consistent across all ParkEase services.
 *
 * errors field is only included when validation failures produce
 * multiple field-level messages — omitted (not null) for single errors.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;       // HTTP status phrase e.g. "Not Found"
    private String message;     // Human-readable description
    private List<String> errors; // Field-level validation errors (DTO @Valid failures)
}