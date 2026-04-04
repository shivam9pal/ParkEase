package com.parkease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailyRevenueResponse {
    private LocalDate date;
    private BigDecimal revenue;
    private long transactionCount;
}