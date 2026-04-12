package com.parkease.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

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
    private Long todayTransactionCount;  // Changed from int to Long for consistency with RevenueDto
    private long todayBookingsCreated;
    private long todayCheckouts;
    private Long averageParkingDurationMinutes;
    private String averageParkingDurationFormatted;
    private List<SpotTypeUtilisationResponse> spotTypeUtilisation;
    private LocalDateTime generatedAt;
}
