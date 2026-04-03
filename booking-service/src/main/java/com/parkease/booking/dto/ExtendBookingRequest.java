package com.parkease.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body for PUT /api/v1/bookings/{bookingId}/extend.
 * newEndTime must be:
 *   1. Not null
 *   2. In the future (basic guard)
 *   3. After the booking's current endTime (enforced in service layer)
 */
@Data
public class ExtendBookingRequest {

    @NotNull(message = "newEndTime is required.")
    @Future(message = "newEndTime must be in the future.")
    private LocalDateTime newEndTime;
}