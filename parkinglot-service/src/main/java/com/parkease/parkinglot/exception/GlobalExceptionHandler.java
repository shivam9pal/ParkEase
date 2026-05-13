package com.parkease.parkinglot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 — Validation errors ──
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        return ResponseEntity.badRequest().body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .message("Validation failed — check the 'errors' field for details")
                .errors(errors)
                .build());
    }

    // ── 404 — Parking lot not found ──
    @ExceptionHandler(ParkingLotNotFoundException.class)
    public ResponseEntity<ApiError> handleParkingLotNotFound(ParkingLotNotFoundException ex) {
        log.warn("Parking lot not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(404)
                .error("Not Found")
                .message(ex.getMessage())
                .build());
    }

    // ── 403 — Invalid access / ownership violation ──
    @ExceptionHandler(InvalidAccessException.class)
    public ResponseEntity<ApiError> handleInvalidAccess(InvalidAccessException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .error("Forbidden")
                .message(ex.getMessage())
                .build());
    }

    // ── 403 — Legacy SecurityException (backward compatibility) ──
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurity(SecurityException ex) {
        log.warn("Security exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .error("Forbidden")
                .message(ex.getMessage())
                .build());
    }

    // ── 409 — No available spots ──
    @ExceptionHandler(NoAvailableSpotsException.class)
    public ResponseEntity<ApiError> handleNoAvailableSpots(NoAvailableSpotsException ex) {
        log.warn("No available spots: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(409)
                .error("Conflict")
                .message(ex.getMessage())
                .build());
    }

    // ── 409 — Legacy IllegalStateException (backward compatibility) ──
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(409)
                .error("Conflict")
                .message(ex.getMessage())
                .build());
    }

    // ── 400 — Invalid argument / malformed request ──
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .message(ex.getMessage())
                .build());
    }

    // ── 500 — Catch-all for unexpected RuntimeExceptions ──
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {
        log.error("Unexpected runtime exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(500)
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please contact support if the problem persists.")
                .build());
    }

    // ── 500 — Catch-all for any other Exceptions ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(500)
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please contact support if the problem persists.")
                .build());
    }
}
