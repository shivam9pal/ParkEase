package com.parkease.booking.dto;

import com.parkease.booking.entity.BookingType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request body for POST /api/v1/bookings.
 *
 * userId is NEVER accepted here — always extracted from JWT in controller.
 * lotId is NOT required — derived from spot lookup (spot knows its lot).
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "spotId is required.")
    private UUID spotId;

    @NotNull(message = "vehicleId is required.")
    private UUID vehicleId;

    @NotNull(message = "bookingType is required. Accepted values: PRE_BOOKING, WALK_IN")
    private BookingType bookingType;

    @NotNull(message = "startTime is required.")
    @Future(message = "startTime must be in the future.")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required.")
    private LocalDateTime endTime;

    // Cross-field validation (endTime > startTime) is enforced
    // in BookingServiceImpl.createBooking() — not via annotation
    // because @Future on endTime alone is insufficient.
}