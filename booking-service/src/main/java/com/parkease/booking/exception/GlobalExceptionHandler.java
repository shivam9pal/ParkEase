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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for booking-service.
 *
 * Type-based exception routing — each specific exception type gets its own
 * @ExceptionHandler with embedded errorCode and HTTP status.
 *
 * No stack traces are exposed to clients — only structured ApiError JSON with
 * errorCode and correlationId for frontend consumption and debugging.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── VEHICLE EXCEPTIONS ──────────────────────────────────────────────────
    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ApiError> handleVehicleNotFound(VehicleNotFoundException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Vehicle Not Found - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(),
                "VEHICLE_NOT_FOUND", correlationId);
    }

    @ExceptionHandler(VehicleNotOwnedException.class)
    public ResponseEntity<ApiError> handleVehicleNotOwned(VehicleNotOwnedException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Vehicle Not Owned - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(),
                "VEHICLE_NOT_OWNED", correlationId);
    }

    @ExceptionHandler(VehicleDeactivatedException.class)
    public ResponseEntity<ApiError> handleVehicleDeactivated(VehicleDeactivatedException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Vehicle Deactivated - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "VEHICLE_DEACTIVATED", correlationId);
    }

    @ExceptionHandler(VehicleServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleVehicleServiceUnavailable(VehicleServiceUnavailableException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Vehicle Service Unavailable - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "VEHICLE_SERVICE_UNAVAILABLE", correlationId);
    }

    // ─── SPOT EXCEPTIONS ────────────────────────────────────────────────────
    @ExceptionHandler(SpotNotFoundException.class)
    public ResponseEntity<ApiError> handleSpotNotFound(SpotNotFoundException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Spot Not Found - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(),
                "SPOT_NOT_FOUND", correlationId);
    }

    @ExceptionHandler(SpotUnavailableException.class)
    public ResponseEntity<ApiError> handleSpotUnavailable(SpotUnavailableException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Spot Unavailable - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                "SPOT_UNAVAILABLE", correlationId);
    }

    @ExceptionHandler(SpotTypeIncompatibleException.class)
    public ResponseEntity<ApiError> handleSpotTypeIncompatible(SpotTypeIncompatibleException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Spot Type Incompatible - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "SPOT_TYPE_INCOMPATIBLE", correlationId);
    }

    @ExceptionHandler(SpotNoEVChargingException.class)
    public ResponseEntity<ApiError> handleSpotNoEVCharging(SpotNoEVChargingException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Spot No EV Charging - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "SPOT_NO_EV_CHARGING", correlationId);
    }

    @ExceptionHandler(SpotReservationException.class)
    public ResponseEntity<ApiError> handleSpotReservation(SpotReservationException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Spot Reservation Failed - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                "SPOT_RESERVATION_FAILED", correlationId);
    }

    @ExceptionHandler(SpotOccupationException.class)
    public ResponseEntity<ApiError> handleSpotOccupation(SpotOccupationException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Spot Occupation Failed - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "SPOT_OCCUPATION_FAILED", correlationId);
    }

    @ExceptionHandler(SpotServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleSpotServiceUnavailable(SpotServiceUnavailableException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Spot Service Unavailable - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "SPOT_SERVICE_UNAVAILABLE", correlationId);
    }

    // ─── BOOKING EXCEPTIONS ─────────────────────────────────────────────────
    @ExceptionHandler(InvalidBookingTimeException.class)
    public ResponseEntity<ApiError> handleInvalidBookingTime(InvalidBookingTimeException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Invalid Booking Time - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "INVALID_BOOKING_TIME", correlationId);
    }

    @ExceptionHandler(BookingStateException.class)
    public ResponseEntity<ApiError> handleBookingState(BookingStateException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Booking State Invalid - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                "BOOKING_STATE_INVALID", correlationId);
    }

    @ExceptionHandler(BookingNotOwnedException.class)
    public ResponseEntity<ApiError> handleBookingNotOwned(BookingNotOwnedException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Booking Not Owned - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(),
                "BOOKING_NOT_OWNED", correlationId);
    }

    @ExceptionHandler(InvalidBookingTypeException.class)
    public ResponseEntity<ApiError> handleInvalidBookingType(InvalidBookingTypeException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Invalid Booking Type - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "INVALID_BOOKING_TYPE", correlationId);
    }

    @ExceptionHandler(BookingSaveException.class)
    public ResponseEntity<ApiError> handleBookingSave(BookingSaveException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Booking Save Failed - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(),
                "BOOKING_SAVE_FAILED", correlationId);
    }

    // ─── LOT EXCEPTIONS ──────────────────────────────────────────────────────
    @ExceptionHandler(LotServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleLotServiceUnavailable(LotServiceUnavailableException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Lot Service Unavailable - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "LOT_SERVICE_UNAVAILABLE", correlationId);
    }

    // ─── FEIGN EXCEPTIONS — Downstream Service Errors ────────────────────────
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ApiError> handleFeignNotFound(FeignException.NotFound ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[ExceptionHandler] Feign 404 Downstream - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.NOT_FOUND,
                "A required resource was not found in a downstream service.",
                "DOWNSTREAM_RESOURCE_NOT_FOUND", correlationId);
    }

    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ApiError> handleFeignServiceUnavailable(FeignException.ServiceUnavailable ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Feign 503 Downstream - Correlation ID: {}", correlationId);
        return buildCustomErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "A required downstream service is currently unavailable. Please try again later.",
                "DOWNSTREAM_SERVICE_UNAVAILABLE", correlationId);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Feign Error (Status={}) - Correlation ID: {}",
                ex.status(), correlationId);
        return buildCustomErrorResponse(HttpStatus.BAD_GATEWAY,
                "An error occurred while communicating with a downstream service. "
                + "Status: " + ex.status(),
                "DOWNSTREAM_SERVICE_ERROR", correlationId);
    }

    // ─── DTO VALIDATION ─────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String correlationId = UUID.randomUUID().toString();
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("[ExceptionHandler] Validation Failed - Correlation ID: {} - Errors: {}",
                correlationId, fieldErrors);

        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed. Check the errors field for details.")
                .errorCode("VALIDATION_FAILED")
                .correlationId(correlationId)
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ─── GENERIC CATCH-ALL ───────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[ExceptionHandler] Unhandled Exception - Correlation ID: {}", correlationId, ex);
        return buildCustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.",
                "INTERNAL_SERVER_ERROR", correlationId);
    }

    // ─── HELPER METHODS ──────────────────────────────────────────────────────
    private ResponseEntity<ApiError> buildCustomErrorResponse(
            HttpStatus status,
            String message,
            String errorCode,
            String correlationId) {
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .errorCode(errorCode)
                .correlationId(correlationId)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}
