package com.parkease.booking.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for booking statistics response. Used by /api/v1/bookings/stats endpoint
 * to return booking counts for a date range. Consumed by analytics-service to
 * populate platform summary.
 */
@Data
@Builder
public class BookingStatsResponse {

    private long activeBookings;           // Current ACTIVE bookings across all lots
    private long completedBookings;        // Bookings with COMPLETED status in the date range
    private long cancelledBookings;        // Bookings with CANCELLED status in the date range
    private LocalDateTime periodStart;     // Start of the query period
    private LocalDateTime periodEnd;       // End of the query period
    private LocalDateTime computedAt;      // When the stats were computed
}
