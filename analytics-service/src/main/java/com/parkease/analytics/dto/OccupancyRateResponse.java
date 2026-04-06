package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OccupancyRateResponse {
    private UUID lotId;
    private Double occupancyRate;
    private Integer availableSpots;
    private Integer totalSpots;
    private LocalDateTime computedAt;
}