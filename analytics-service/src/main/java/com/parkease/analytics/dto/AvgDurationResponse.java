package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AvgDurationResponse {
    private UUID lotId;
    private Long averageDurationMinutes;
    private String averageDurationFormatted;  // "1h 35m"
}