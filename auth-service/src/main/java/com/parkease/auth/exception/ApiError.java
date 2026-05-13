package com.parkease.auth.exception;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;           // Business error code (e.g., "OTP_RATE_LIMITED")
    private String correlationId;       // Request tracking ID for debugging
    private String path;
    private List<String> errors;              // field-level validation errors
}
