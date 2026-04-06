package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlatformSummaryResponse {
    private int totalLots;
    private int totalSpots;
    private int totalAvailableSpots;
    private Double platformOccupancyRate;
    private BigDecimal todayRevenue;
    private int todayTransactionCount;
    private Long platformAvgDurationMinutes;
    private LocalDateTime generatedAt;
}