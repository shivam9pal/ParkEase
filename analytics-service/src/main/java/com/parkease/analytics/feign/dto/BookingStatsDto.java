package com.parkease.analytics.feign.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for booking statistics response from booking-service. Used by
 * getPlatformSummary() to get counts of active, completed, and cancelled
 * bookings.
 */
@Data
@Builder
public class BookingStatsDto {

    private long activeBookings;           // Current ACTIVE bookings
    private long completedBookings;        // Bookings with COMPLETED status in the date range
    private long cancelledBookings;        // Bookings with CANCELLED status in the date range
    private LocalDateTime periodStart;     // Start of the query period
    private LocalDateTime periodEnd;       // End of the query period
    private LocalDateTime computedAt;      // When the stats were computed
}
