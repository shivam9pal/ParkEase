package com.parkease.auth.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors ──────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .correlationId(generateCorrelationId())
                .path(getRequestPath(request))
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // ── Bad credentials ────────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                "INVALID_CREDENTIALS",
                request
        );
    }

    // ── OTP Exceptions ─────────────────────────────────────────────────────────
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtpException(
            OtpException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── Authentication Exceptions ──────────────────────────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── User Management Exceptions ─────────────────────────────────────────────
    @ExceptionHandler(UserManagementException.class)
    public ResponseEntity<ApiError> handleUserManagementException(
            UserManagementException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── Admin Exceptions ───────────────────────────────────────────────────────
    @ExceptionHandler(AdminException.class)
    public ResponseEntity<ApiError> handleAdminException(
            AdminException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── Password Exceptions ────────────────────────────────────────────────────
    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ApiError> handlePasswordException(
            PasswordException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── External Service Exceptions ────────────────────────────────────────────
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        return buildCustomErrorResponse(ex, request);
    }

    // ── Generic exception fallback ─────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                "INTERNAL_ERROR",
                request
        );
    }

    // ── Helper: Build custom error response for BaseAuthException subclasses ───
    private ResponseEntity<ApiError> buildCustomErrorResponse(
            BaseAuthException ex, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getHttpStatus().getReasonPhrase())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .correlationId(generateCorrelationId())
                .path(getRequestPath(request))
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(apiError);
    }

    // ── Helper: Build standard error response ──────────────────────────────────
    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status, String message, String errorCode, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .errorCode(errorCode)
                .correlationId(generateCorrelationId())
                .path(getRequestPath(request))
                .build();

        return ResponseEntity.status(status).body(apiError);
    }

    // ── Helper: Generate unique correlation ID for request tracking ────────────
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // ── Helper: Extract request path for logging ───────────────────────────────
    private String getRequestPath(WebRequest request) {
        String description = request.getDescription(false);
        return description != null ? description.replace("uri=", "") : "";
    }
}
