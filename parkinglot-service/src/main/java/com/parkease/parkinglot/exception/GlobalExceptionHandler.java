package com.parkease.parkinglot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 — Validation errors ──
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .message("Validation failed — check the 'errors' field for details")
                .errors(errors)
                .build());
    }

    // ── 403 — Ownership / access violation ──
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .error("Forbidden")
                .message(ex.getMessage())
                .build());
    }

    // ── 409 — No available spots ──
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(409)
                .error("Conflict")
                .message(ex.getMessage())
                .build());
    }

    // ── 404 / 400 / 500 — RuntimeException routing ──
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        HttpStatus status;
        String error;

        if (msg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
            error  = "Not Found";
        } else if (msg.contains("already") || msg.contains("invalid")) {
            status = HttpStatus.BAD_REQUEST;
            error  = "Bad Request";
        } else if (msg.contains("expired") || msg.contains("unauthorized")) {
            status = HttpStatus.UNAUTHORIZED;
            error  = "Unauthorized";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            error  = "Internal Server Error";
        }

        return ResponseEntity.status(status).body(ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(ex.getMessage())
                .build());
    }
}