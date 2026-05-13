package com.parkease.analytics.exception;

import feign.FeignException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ═════════════════════════════════════════════════════════════════
    // CUSTOM ANALYTICS EXCEPTIONS
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        log.warn("[SECURITY AUDIT] Unauthorized access attempt - userId: {}, resource: {} (id: {}), message: {}",
                ex.getUserId(), ex.getResourceType(), ex.getResourceId(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
        log.error("[EXTERNAL SERVICE] {} failed with status {}: {}",
                ex.getServiceName(), ex.getHttpStatus(), ex.getMessage());

        // Preserve the original HTTP status from the external service
        HttpStatus status = HttpStatus.resolve(ex.getHttpStatus());
        if (status == null || status.is5xxServerError()) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return buildErrorResponse(status,
                String.format("Service %s is currently unavailable. Please try again later.", ex.getServiceName()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrors());
    }

    // ═════════════════════════════════════════════════════════════════
    // SPRING SECURITY EXCEPTIONS
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[SECURITY] Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[SECURITY] Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials provided");
    }

    // ═════════════════════════════════════════════════════════════════
    // FEIGN CLIENT EXCEPTIONS
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleFeignNotFound(FeignException.NotFound ex) {
        log.warn("Downstream service returned 404: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested resource was not found in downstream service");
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<ErrorResponse> handleFeignUnauthorized(FeignException.Unauthorized ex) {
        log.error("[CRITICAL] Unauthorized access to downstream service - Check JWT/Auth configuration");
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Service authentication failed. Please contact support.");
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponse> handleFeignForbidden(FeignException.Forbidden ex) {
        log.error("[CRITICAL] Forbidden access to downstream service: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Service access denied. Please contact support.");
    }

    @ExceptionHandler(FeignException.BadRequest.class)
    public ResponseEntity<ErrorResponse> handleFeignBadRequest(FeignException.BadRequest ex) {
        log.error("Bad request to downstream service: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Invalid request sent to downstream service");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignError(FeignException ex) {
        log.error("[EXTERNAL SERVICE ERROR] Feign status: {}, message: {}", ex.status(), ex.getMessage());

        // Map Feign status to HTTP status
        HttpStatus status = HttpStatus.valueOf(ex.status());
        if (status.is5xxServerError()) {
            return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                    "External service is temporarily unavailable. Please try again later.");
        }

        return buildErrorResponse(status, "External service error occurred");
    }

    // ═════════════════════════════════════════════════════════════════
    // REQUEST VALIDATION EXCEPTIONS
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Aggregate ALL validation errors, not just the first
        List<Map<String, String>> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage()
        ))
                .collect(Collectors.toList());

        log.warn("Validation failed with {} errors", validationErrors.size());

        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Request validation failed", validationErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.warn("Missing parameter: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("HTTP method %s is not supported for this endpoint", ex.getMethod());
        log.warn("Method not supported: {}", message);
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        String message = String.format("Endpoint not found: %s %s", ex.getHttpMethod(), ex.getRequestURL());
        log.warn("Endpoint not found: {}", message);
        return buildErrorResponse(HttpStatus.NOT_FOUND, message);
    }

    // ═════════════════════════════════════════════════════════════════
    // DATABASE EXCEPTIONS
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.CONFLICT,
                "Request violates data integrity constraints. Please check your input.");
    }

    // ═════════════════════════════════════════════════════════════════
    // GENERIC EXCEPTION HANDLER (LAST RESORT)
    // ═════════════════════════════════════════════════════════════════
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("[UNEXPECTED ERROR] Unhandled RuntimeException - Type: {}, Message: {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("[CRITICAL] Unhandled Exception - Type: {}, Message: {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.");
    }

    // ═════════════════════════════════════════════════════════════════
    // RESPONSE BUILDERS
    // ═════════════════════════════════════════════════════════════════
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message,
            List<Map<String, String>> validationErrors) {
        HttpServletRequest request = getRequest();
        String path = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .method(method)
                .requestId(UUID.randomUUID().toString())
                .validationErrors(validationErrors)
                .build();

        log.debug("Returning error response: {} - {} [{}:{}]",
                status.value(), message, method, path);

        return ResponseEntity.status(status).body(response);
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
