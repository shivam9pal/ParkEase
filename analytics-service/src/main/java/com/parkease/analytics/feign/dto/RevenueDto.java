package com.parkease.analytics.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueDto {
    private UUID lotId;               // null for platform-wide revenue
    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal totalRevenue;
    private String currency;
    private Integer transactionCount;
}