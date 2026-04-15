package com.parkease.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotSummaryAnalyticsResponse {

    private UUID lotId;
    private String lotName;
    private Integer totalSpots;
    private Integer availableSpots;
    private Double currentOccupancyRate;
    private BigDecimal todayRevenue;
    private Long todayTransactionCount;
    private Long averageParkingDurationMinutes;
    private Double peakOccupancyRate;
    private LocalDateTime generatedAt;
}
