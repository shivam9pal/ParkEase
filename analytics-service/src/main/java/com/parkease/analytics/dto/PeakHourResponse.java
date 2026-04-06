package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PeakHourResponse {
    private int hour;
    private Double averageOccupancyRate;
    private String label;   // e.g. "18:00 - 19:00"
}