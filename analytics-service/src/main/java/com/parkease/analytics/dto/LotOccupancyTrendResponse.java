package com.parkease.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotOccupancyTrendResponse {

    private UUID lotId;
    private String period;                    // DAILY/WEEKLY/MONTHLY
    private Double averageOccupancyRate;
    private Double peakOccupancyRate;
    private Double minOccupancyRate;
    private Integer totalSpots;
    private Integer averageAvailableSpots;
    private List<HourlyOccupancyResponse> hourlyBreakdown;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime computedAt;
}
