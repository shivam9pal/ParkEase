package com.parkease.spot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for spot-service.
 *
 * <p>
 * Catches all exceptions thrown from any layer (service, controller) and
 * translates them into structured {@link ApiError} JSON responses. No raw stack
 * traces are ever exposed to API callers.
 *
 * <p>
 * Exception → HTTP Status mapping:
 * <pre>
 *   MethodArgumentNotValidException      → 400  (DTO validation failures)
 *   DuplicateSpotException               → 400  (duplicate spot number)
 *   IllegalArgumentException             → 400  (other bad input)
 *   MethodArgumentTypeMismatchException  → 400  (invalid UUID / enum in path)
 *   SpotNotFoundException                → 404  (spot not found)
 *   ParkingLotNotFoundException          → 404  (parking lot not found)
 *   InvalidSpotStatusException           → 409  (invalid status transition)
 *   DataIntegrityViolationException      → 409  (database constraint violation)
 *   AccessDeniedException                → 403  (insufficient role)
 *   AuthenticationException              → 401  (missing / invalid JWT)
 *   Exception (catch-all)                → 500  (unexpected errors)
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DUPLICATE_SPOT_CODE = "DUPLICATE_SPOT_NUMBER";
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";
    private static final String INVALID_TYPE_CODE = "INVALID_TYPE";
    private static final String RESOURCE_NOT_FOUND_CODE = "RESOURCE_NOT_FOUND";
    private static final String LOT_NOT_FOUND_CODE = "LOT_NOT_FOUND";
    private static final String INVALID_STATUS_CODE = "INVALID_STATUS_TRANSITION";
    private static final String CONSTRAINT_VIOLATION_CODE = "CONSTRAINT_VIOLATION";
    private static final String BAD_REQUEST_CODE = "BAD_REQUEST";
    private static final String FORBIDDEN_CODE = "FORBIDDEN";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";

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
                VALIDATION_FAILED_CODE,
                "Validation failed — check the errors field for details",
                fieldErrors
        );
    }

    // ── 400 — Duplicate spot number ──────────────────────────────────────────
    @ExceptionHandler(DuplicateSpotException.class)
    public ResponseEntity<ApiError> handleDuplicateSpot(
            DuplicateSpotException ex) {

        log.warn("Duplicate spot number: {} in lot: {}", ex.getSpotNumber(), ex.getLotId());
        return build(
                HttpStatus.BAD_REQUEST,
                DUPLICATE_SPOT_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 400 — Generic illegal argument (fallback) ────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Bad request: {}", ex.getMessage());
        return build(
                HttpStatus.BAD_REQUEST,
                BAD_REQUEST_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 400 — Invalid UUID or Enum value in path / query param ───────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Class<?> requiredType = ex.getRequiredType();
        String typeName = (requiredType == null) ? "unknown" : requiredType.getSimpleName();
        String message = String.format(
                "Invalid value '%s' for parameter '%s' — expected type: %s",
                ex.getValue(),
                ex.getName(),
                typeName
        );

        log.warn("Type mismatch: {}", message);
        return build(
                HttpStatus.BAD_REQUEST,
                INVALID_TYPE_CODE,
                message,
                List.of()
        );
    }

    // ── 404 — Spot not found ──────────────────────────────────────────────────
    @ExceptionHandler(SpotNotFoundException.class)
    public ResponseEntity<ApiError> handleSpotNotFound(
            SpotNotFoundException ex) {

        log.warn("Spot not found: {}", ex.getSpotId());
        return build(
                HttpStatus.NOT_FOUND,
                RESOURCE_NOT_FOUND_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 404 — Parking lot not found ───────────────────────────────────────────
    @ExceptionHandler(ParkingLotNotFoundException.class)
    public ResponseEntity<ApiError> handleLotNotFound(
            ParkingLotNotFoundException ex) {

        log.warn("Parking lot not found: {}", ex.getLotId());
        return build(
                HttpStatus.NOT_FOUND,
                LOT_NOT_FOUND_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 409 — Invalid spot status transition ─────────────────────────────────
    @ExceptionHandler(InvalidSpotStatusException.class)
    public ResponseEntity<ApiError> handleInvalidSpotStatus(
            InvalidSpotStatusException ex) {

        log.warn("Invalid spot status transition: spotId={}, currentStatus={}, operation={}",
                ex.getSpotId(), ex.getCurrentStatus(), ex.getAttemptedOperation());
        return build(
                HttpStatus.CONFLICT,
                INVALID_STATUS_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 409 — Fallback for generic illegal state exceptions ───────────────────
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex) {

        log.warn("Conflict — invalid state: {}", ex.getMessage());
        return build(
                HttpStatus.CONFLICT,
                INVALID_STATUS_CODE,
                ex.getMessage(),
                List.of()
        );
    }

    // ── 409 — Database constraint violations ───────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        log.warn("Database constraint violation: {}", ex.getMessage());
        return build(
                HttpStatus.CONFLICT,
                CONSTRAINT_VIOLATION_CODE,
                "Database integrity violation — likely a duplicate or constraint issue",
                List.of()
        );
    }

    // ── 403 — Insufficient role (DRIVER trying to add a spot etc.) ───────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        return build(
                HttpStatus.FORBIDDEN,
                FORBIDDEN_CODE,
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
                UNAUTHORIZED_CODE,
                "Authentication required — provide a valid JWT Bearer token.",
                List.of()
        );
    }

    // ── 500 — Catch-all for anything not handled above ───────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        String errorId = generateErrorId();
        log.error("Unexpected error [{}]: {}", errorId, ex.getMessage(), ex);
        return buildWithErrorId(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE,
                "An unexpected error occurred. Please contact support with error ID: " + errorId,
                List.of(),
                errorId
        );
    }

    // ─────────────────────────────── Builder helpers ──────────────────────────
    /**
     * Builds an ApiError response with status, code, message, and validation
     * errors.
     *
     * @param status HTTP status code
     * @param code Error code for client-side classification
     * @param message Human-readable error message
     * @param errors List of field validation errors (empty for non-validation
     * errors)
     * @return ResponseEntity with ApiError body
     */
    private ResponseEntity<ApiError> build(
            HttpStatus status, String code, String message, List<String> errors) {

        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .code(code)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Builds an ApiError response for 500 errors with a unique tracking ID.
     *
     * @param status HTTP status code
     * @param code Error code for client-side classification
     * @param message Human-readable error message
     * @param errors List of field validation errors
     * @param errorId Unique tracking ID for this error
     * @return ResponseEntity with ApiError body
     */
    private ResponseEntity<ApiError> buildWithErrorId(
            HttpStatus status, String code, String message, List<String> errors, String errorId) {

        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .code(code)
                .errorId(errorId)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Generates a unique error ID for tracking 500 errors in logs. Format:
     * ERR-<YYYYMMDD>-<6 random alphanumeric>
     *
     * @return Unique error tracking ID
     */
    private String generateErrorId() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = generateRandomString(6);
        return "ERR-" + dateStr + "-" + randomStr;
    }

    /**
     * Generates a random alphanumeric string of specified length.
     *
     * @param length Length of the random string
     * @return Random alphanumeric string
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
