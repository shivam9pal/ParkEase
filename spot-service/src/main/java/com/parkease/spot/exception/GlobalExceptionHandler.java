package com.parkease.spot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for spot-service.
 *
 * <p>Catches all exceptions thrown from any layer (service, controller)
 * and translates them into structured {@link ApiError} JSON responses.
 * No raw stack traces are ever exposed to API callers.
 *
 * <p>Exception → HTTP Status mapping:
 * <pre>
 *   MethodArgumentNotValidException    → 400  (DTO validation failures)
 *   IllegalArgumentException           → 400  (duplicate spotNumber, bad input)
 *   MethodArgumentTypeMismatchException→ 400  (invalid UUID / enum in path)
 *   RuntimeException "not found"       → 404  (spot not found)
 *   IllegalStateException              → 409  (invalid status transition)
 *   AccessDeniedException              → 403  (wrong role)
 *   AuthenticationException            → 401  (missing / invalid JWT)
 *   Exception (catch-all)              → 500  (unexpected)
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 — DTO / Bean Validation failures ─────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex) {

        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", fieldErrors);

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed — check the errors field for details",
                fieldErrors
        );
    }

    // ── 400 — Duplicate spotNumber, business-rule bad input ──────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    // ── 400 — Invalid UUID or Enum value in path / query param ───────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s' — expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("Type mismatch: {}", message);
        return build(HttpStatus.BAD_REQUEST, message, List.of());
    }

    // ── 404 — Spot not found ──────────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {

        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (msg.contains("not found")) {
            log.warn("Resource not found: {}", ex.getMessage());
            return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
        }

        // Unknown RuntimeException → 500
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.",
                List.of()
        );
    }

    // ── 409 — Invalid spot status transition ─────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex) {

        log.warn("Conflict — invalid state transition: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    // ── 403 — Insufficient role (DRIVER trying to add a spot etc.) ───────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        return build(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.",
                List.of()
        );
    }

    // ── 401 — Missing or invalid JWT ─────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());
        return build(
                HttpStatus.UNAUTHORIZED,
                "Authentication required — provide a valid JWT Bearer token.",
                List.of()
        );
    }

    // ── 500 — Catch-all for anything not handled above ───────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.",
                List.of()
        );
    }

    // ─────────────────────────────── Builder helper ──────────────────────────

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, List<String> errors) {

        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}