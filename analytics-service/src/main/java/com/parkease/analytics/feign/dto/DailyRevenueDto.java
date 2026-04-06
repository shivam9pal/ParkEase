package com.parkease.analytics.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyRevenueDto {
    private String date;              // "2026-04-01" (LocalDate serialised as String)
    private BigDecimal revenue;
    private Integer transactionCount;
}