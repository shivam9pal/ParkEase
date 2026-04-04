package com.parkease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private BigDecimal amount;
    private String status;
    private String mode;
    private String transactionId;
    private String currency;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String description;
    private LocalDateTime createdAt;
    // receiptPath intentionally excluded
}