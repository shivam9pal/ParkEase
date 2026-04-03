package com.parkease.vehicle.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for vehicle-service.
 *
 * Maps every exception type to a clean ApiError JSON response with
 * an appropriate HTTP status code. No stack traces ever reach the client.
 *
 * HTTP Status mapping:
 *  201 CREATED            — returned directly by VehicleResource on success
 *  200 OK                 — returned directly by VehicleResource on success
 *  400 BAD REQUEST        — validation failures, duplicate plate, bad input
 *  401 UNAUTHORIZED       — missing/invalid/expired JWT token
 *  403 FORBIDDEN          — authenticated but insufficient permission
 *  404 NOT FOUND          — vehicle does not exist
 *  409 CONFLICT           — DB-level unique constraint violation
 *  500 INTERNAL ERROR     — unexpected errors
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────────────────────
    // 400 — @Valid / @Validated input validation failures
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity.badRequest().body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message("Validation failed — check the 'errors' field for details")
                        .errors(fieldErrors)
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 401 — Spring Security authentication failures
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error("Unauthorized")
                        .message("Authentication required — provide a valid JWT Bearer token")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 403 — Authenticated but accessing another driver's resource
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.FORBIDDEN.value())
                        .error("Forbidden")
                        .message("You do not have permission to access this resource")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 — DB-level unique constraint (e.g., duplicate plate+owner combo)
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message("A vehicle with this license plate is already registered to your account")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 400 / 401 / 403 / 404 — RuntimeException (message-driven routing)
    // Follows the same pattern as auth-service GlobalExceptionHandler
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        log.warn("RuntimeException: {}", ex.getMessage());

        // 404 — entity not found
        if (message.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiError.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.NOT_FOUND.value())
                            .error("Not Found")
                            .message(ex.getMessage())
                            .build()
            );
        }

        // 400 — duplicate, already exists, already registered
        if (message.contains("already registered") ||
                message.contains("already exists") ||
                message.contains("duplicate")) {
            return ResponseEntity.badRequest().body(
                    ApiError.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .error("Bad Request")
                            .message(ex.getMessage())
                            .build()
            );
        }

        // 403 — forbidden / access denied
        if (message.contains("forbidden") ||
                message.contains("not allowed") ||
                message.contains("access denied")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiError.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.FORBIDDEN.value())
                            .error("Forbidden")
                            .message(ex.getMessage())
                            .build()
            );
        }

        // 401 — token issues
        if (message.contains("expired") ||
                message.contains("invalid token") ||
                message.contains("unauthorized")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiError.builder()
                            .timestamp(LocalDateTime.now())
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .error("Unauthorized")
                            .message(ex.getMessage())
                            .build()
            );
        }

        // 500 — default fallback
        log.error("Unhandled RuntimeException", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message("An unexpected error occurred. Please try again.")
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 500 — catch-all for anything not caught above
    // ─────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message("An unexpected error occurred. Please contact support.")
                        .build()
        );
    }
}