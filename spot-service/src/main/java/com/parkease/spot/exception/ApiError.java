package com.parkease.spot.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response body returned by GlobalExceptionHandler.
 *
 * <p>Shape matches the ApiError contract used across all ParkEase services
 * so the React frontend and inter-service callers can handle errors uniformly.
 *
 * <pre>
 * {
 *   "timestamp": "2026-04-03T20:00:00",
 *   "status":    409,
 *   "error":     "Conflict",
 *   "message":   "Spot <uuid> is already RESERVED — cannot reserve again",
 *   "errors":    []          ← populated only for validation failures
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /** HTTP status code — e.g. 400, 404, 409 */
    private int status;

    /** HTTP status reason phrase — e.g. "Bad Request", "Not Found", "Conflict" */
    private String error;

    /** Human-readable description of what went wrong */
    private String message;

    /**
     * Field-level validation errors.
     * Populated by MethodArgumentNotValidException handler.
     * Empty list for all other error types.
     */
    private List<String> errors;
}