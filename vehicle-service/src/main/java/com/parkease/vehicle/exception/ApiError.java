package com.parkease.vehicle.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response body returned by all vehicle-service error cases.
 *
 * Example 404:
 * {
 *   "timestamp": "2026-04-03T02:00:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Vehicle not found with id: ...",
 *   "errors": null
 * }
 *
 * Example 400 (validation):
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "errors": ["licensePlate: License plate is required", "vehicleType: Vehicle type is required"]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit null fields from JSON output
public class ApiError {

    private LocalDateTime timestamp;
    private int           status;
    private String        error;
    private String        message;

    // Populated only for @Valid validation failures
    private List<String>  errors;
}