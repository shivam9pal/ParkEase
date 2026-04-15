package com.parkease.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformSummaryResponse {

    private int totalLots;
    private int totalSpots;
    private int totalAvailableSpots;
    private Double platformOccupancyRate;
    private BigDecimal todayRevenue;
    private Long todayTransactionCount;  // Changed from int to Long for consistency with RevenueDto
    private Long platformAvgDurationMinutes;

    // NEW FIELDS — User Management & Booking Statistics
    private Long totalUsers;               // Total DRIVER + MANAGER users in the system
    private Long pendingLots;              // Count of non-approved parking lots
    private Long activeBookings;           // Current ACTIVE bookings across all lots
    private Long completedBookings;        // Bookings completed today
    private Long cancelledBookings;        // Bookings cancelled today

    private LocalDateTime generatedAt;
}
