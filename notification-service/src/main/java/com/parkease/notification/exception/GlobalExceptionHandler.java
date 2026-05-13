package com.parkease.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ═════════════════════════════════════════════════════════════
    // CUSTOM DOMAIN EXCEPTIONS (Notification Service)
    // ═════════════════════════════════════════════════════════════
    // ─── NotificationNotFoundException (404) ───
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFound(
            NotificationNotFoundException ex) {

        log.warn("Notification not found: {}", ex.getReason());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 404);
        body.put("error", "Notification Not Found");
        body.put("message", ex.getReason());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ─── InvalidNotificationException (403/400) ───
    @ExceptionHandler(InvalidNotificationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidNotification(
            InvalidNotificationException ex) {

        log.warn("Invalid notification operation: {}", ex.getReason());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", "Invalid Notification Operation");
        body.put("message", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // ─── UserFetchException (503) ───
    @ExceptionHandler(UserFetchException.class)
    public ResponseEntity<Map<String, Object>> handleUserFetchException(
            UserFetchException ex) {

        log.error("Auth service unavailable: {}", ex.getReason());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("message", "Could not fetch user details. Auth service is temporarily unavailable.");
        body.put("detail", ex.getReason());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    // ─── ChannelDispatchException (500 or 207) ───
    @ExceptionHandler(ChannelDispatchException.class)
    public ResponseEntity<Map<String, Object>> handleChannelDispatchException(
            ChannelDispatchException ex) {

        log.error("Channel dispatch error: {} (success: {}, failed: {})",
                ex.getReason(), ex.getSuccessCount(), ex.getFailureCount());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "Notification Dispatch Error");
        body.put("message", ex.getReason());
        body.put("channelsSucceeded", ex.getSuccessCount());
        body.put("channelsFailed", ex.getFailureCount());

        if (ex.isPartialFailure()) {
            // Partial success — 207 Multi-Status
            body.put("status", 207);
            return ResponseEntity.status(207).body(body);
        } else {
            // Complete failure — 500
            body.put("status", 500);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // SPRING & VALIDATION EXCEPTIONS
    // ═════════════════════════════════════════════════════════════
    // ─── Validation errors (@Valid) ───
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed for request: {}", fieldErrors);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // ─── Malformed request body (invalid JSON) ───
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "Request body is malformed or invalid JSON");

        return ResponseEntity.badRequest().body(body);
    }

    // ─── Illegal argument (programming error) ───
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(body);
    }

    // ─── Database errors (DataAccessException) ───
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex) {

        log.error("Database error: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 500);
        body.put("error", "Database Error");
        body.put("message", "Could not process request due to a database error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ─── ResponseStatusException (404, 403, etc.) ───
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex) {

        log.warn("Response status exception: {} — {}", ex.getStatusCode(), ex.getReason());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", "HTTP " + ex.getStatusCode().value());
        body.put("message", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // ═════════════════════════════════════════════════════════════
    // CATCH-ALL (least specific — last)
    // ═════════════════════════════════════════════════════════════
    // ─── Catch-all for any unhandled exceptions ───
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
