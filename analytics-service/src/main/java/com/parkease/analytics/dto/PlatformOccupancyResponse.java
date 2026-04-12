package com.parkease.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformOccupancyResponse {

    private Double averageOccupancyRate;
    private Double peakOccupancyRate;
    private Double minOccupancyRate;
    private Integer totalLotsAnalyzed;
    private Integer totalSpots;
    private Integer averageAvailableSpots;
    private String period;
    private List<HourlyOccupancyResponse> hourlyBreakdown;
    private LocalDateTime computedAt;
}
