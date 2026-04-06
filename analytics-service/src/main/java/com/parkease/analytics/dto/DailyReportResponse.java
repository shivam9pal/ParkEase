package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DailyReportResponse {
    private UUID lotId;
    private LocalDate reportDate;
    private Double currentOccupancyRate;
    private Integer availableSpots;
    private Integer totalSpots;
    private List<PeakHourResponse> peakHours;
    private BigDecimal todayRevenue;
    private int todayTransactionCount;
    private long todayBookingsCreated;
    private long todayCheckouts;
    private Long averageParkingDurationMinutes;
    private String averageParkingDurationFormatted;
    private List<SpotTypeUtilisationResponse> spotTypeUtilisation;
    private LocalDateTime generatedAt;
}