package com.parkease.booking.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for booking-service.
 *
 * Exception → HTTP Status mapping:
 *   RuntimeException("not found")        → 404 NOT FOUND
 *   IllegalStateException                → 409 CONFLICT
 *   SecurityException                    → 403 FORBIDDEN
 *   IllegalArgumentException             → 400 BAD REQUEST
 *   FeignException.NotFound              → 404 (downstream resource missing)
 *   FeignException.ServiceUnavailable    → 503 SERVICE UNAVAILABLE
 *   FeignException (other)               → 502 BAD GATEWAY
 *   MethodArgumentNotValidException      → 400 BAD REQUEST (with field errors)
 *   Exception (catch-all)                → 500 INTERNAL SERVER ERROR
 *
 * No stack traces are exposed to clients — only structured ApiError JSON.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── 404: Resource Not Found ──────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Route to 404 if message contains "not found"
        if (message.contains("not found")) {
            log.warn("[ExceptionHandler] 404 Not Found: {}", ex.getMessage());
            return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        // Route to 503 if message contains "unavailable"
        if (message.contains("unavailable")) {
            log.error("[ExceptionHandler] 503 Service Unavailable: {}", ex.getMessage());
            return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }

        // All other RuntimeExceptions → 500
        log.error("[ExceptionHandler] 500 RuntimeException: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    // ─── 409: Invalid State Transition ────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException ex) {
        log.warn("[ExceptionHandler] 409 Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ─── 403: Ownership / Authorization Violation ─────────────────────────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurityException(SecurityException ex) {
        log.warn("[ExceptionHandler] 403 Forbidden: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ─── 400: Invalid Input ───────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[ExceptionHandler] 400 Bad Request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─── 400: DTO Validation Failures (@Valid) ────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("[ExceptionHandler] 400 Validation failed: {}", fieldErrors);

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed. Check the errors field for details.")
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ─── Feign Exceptions — Downstream Service Errors ─────────────────────────

    /**
     * 404 from a downstream service — e.g., spot or vehicle not found.
     * Translate to a meaningful 404 for the booking-service client.
     */
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ApiError> handleFeignNotFound(FeignException.NotFound ex) {
        log.warn("[ExceptionHandler] Feign 404 from downstream: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND,
                "A required resource was not found in a downstream service.");
    }

    /**
     * 503 from a downstream service — service is down or circuit open.
     */
    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ApiError> handleFeignServiceUnavailable(FeignException.ServiceUnavailable ex) {
        log.error("[ExceptionHandler] Feign 503 downstream unavailable: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "A required downstream service is currently unavailable. Please try again later.");
    }

    /**
     * All other Feign exceptions — connection refused, timeout, 5xx from downstream.
     * Return 502 BAD GATEWAY — tells the client the error is upstream, not here.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException ex) {
        log.error("[ExceptionHandler] Feign error (status={}): {}",
                ex.status(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY,
                "An error occurred while communicating with a downstream service. " +
                        "Status: " + ex.status());
    }

    // ─── 500: Catch-All ───────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("[ExceptionHandler] 500 Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    // ─── Builder Helper ───────────────────────────────────────────────────────

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message) {
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}