package com.parkease.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for GET /api/v1/bookings/{bookingId}/fare
 *
 * Provides a real-time fare estimate for an ACTIVE booking.
 * Does NOT mutate the booking — read-only operation.
 * totalAmount on the booking stays null until actual checkOut().
 *
 * Formula:
 *   estimatedHours = max(1.0, minutesSinceCheckIn / 60.0)
 *   estimatedFare  = pricePerHour × estimatedHours  (rounded HALF_UP, 2dp)
 */
@Data
@Builder
public class FareCalculationResponse {

    private UUID bookingId;

    // Snapshotted price — same value stored in Booking.pricePerHour
    private BigDecimal pricePerHour;

    // Billable hours after minimum-1-hour enforcement
    private BigDecimal estimatedHours;

    // estimatedHours × pricePerHour
    private BigDecimal estimatedFare;

    // Actual check-in time — helps client understand the billing window
    private LocalDateTime checkInTime;

    // Timestamp when this estimate was generated — for display ("as of HH:MM")
    private LocalDateTime calculatedAt;
}