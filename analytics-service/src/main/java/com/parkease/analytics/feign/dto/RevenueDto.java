package com.parkease.analytics.feign.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueDto {

    private UUID lotId;               // null for platform-wide revenue
    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal totalRevenue;
    private String currency;
    private Long transactionCount;    // Changed from Integer to Long (matches PaymentService RevenueResponse)
}
