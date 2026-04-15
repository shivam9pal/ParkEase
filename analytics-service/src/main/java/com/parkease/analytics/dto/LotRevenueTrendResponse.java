package com.parkease.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotRevenueTrendResponse {

    private UUID lotId;
    private String period;                    // DAILY/WEEKLY/MONTHLY
    private BigDecimal totalRevenue;
    private String currency;
    private Long transactionCount;
    private LocalDateTime periodStart;        // When the period started
    private LocalDateTime periodEnd;          // When the period ends
    private LocalDateTime computedAt;
}
