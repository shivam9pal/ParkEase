package com.parkease.auth.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (EXISTING — unchanged) ──────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    // ── Bad credentials (EXISTING — unchanged) ────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password", null);
    }

    // ── Runtime exceptions (EXISTING base + all new mappings added) ───────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage();
        HttpStatus status = resolveStatus(msg);
        return buildError(status, msg, null);
    }

    // ── Status resolution — ordered from most specific to least specific ───────
    private HttpStatus resolveStatus(String msg) {
        if (msg == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // ── 403 Forbidden ──────────────────────────────────────────────────────
        if (msg.contains("Registration as ADMIN is not allowed")) {
            return HttpStatus.FORBIDDEN;
        }
        if (msg.contains("Only Super Admin can")) {
            return HttpStatus.FORBIDDEN;
        }
        if (msg.contains("Super Admin cannot be deleted")) {
            return HttpStatus.FORBIDDEN;
        }

        // ── 429 Too Many Requests ──────────────────────────────────────────────
        if (msg.contains("Too many OTP requests")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (msg.contains("Too many wrong attempts")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (msg.contains("Please wait")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        // ── 410 Gone ───────────────────────────────────────────────────────────
        if (msg.contains("OTP has expired")) {
            return HttpStatus.GONE;
        }

        // ── 404 Not Found ──────────────────────────────────────────────────────
        if (msg.contains("OTP not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (msg.contains("No account found with this email")) {
            return HttpStatus.NOT_FOUND;
        }
        if (msg.contains("Admin not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (msg.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }

        // ── 502 Bad Gateway ────────────────────────────────────────────────────
        if (msg.contains("Failed to send OTP email")) {
            return HttpStatus.BAD_GATEWAY;
        }

        // ── 401 Unauthorized ───────────────────────────────────────────────────
        if (msg.contains("Invalid Admin password")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (msg.contains("Admin account is deactivated")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (msg.contains("deactivated")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (msg.contains("expired")) {
            return HttpStatus.UNAUTHORIZED;
        }

        // ── 400 Bad Request ────────────────────────────────────────────────────
        if (msg.contains("Invalid OTP")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("OTP already used")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("OTP session expired")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("Please verify your email")) {
            return HttpStatus.FORBIDDEN;
        }
        if (msg.contains("Email not verified")) {
            return HttpStatus.FORBIDDEN;
        }
        if (msg.contains("uses Google/GitHub login")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("Password reset is not available")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("Admin with this email already exists")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("already registered")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (msg.contains("incorrect")) {
            return HttpStatus.BAD_REQUEST;
        }

        // ── 500 fallback ───────────────────────────────────────────────────────
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // ── Response builder (EXISTING — unchanged) ───────────────────────────────
    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, List<String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (errors != null) {
            body.put("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
