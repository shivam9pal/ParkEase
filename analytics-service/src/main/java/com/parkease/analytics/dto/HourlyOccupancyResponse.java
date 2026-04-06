package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HourlyOccupancyResponse {
    private int hour;                     // 0–23
    private Double averageOccupancyRate;  // avg % across last 30 days at this hour
}