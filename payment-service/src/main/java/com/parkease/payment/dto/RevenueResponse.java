package com.parkease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RevenueResponse {
    private UUID lotId;
    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal totalRevenue;
    private String currency;
    private long transactionCount;
}