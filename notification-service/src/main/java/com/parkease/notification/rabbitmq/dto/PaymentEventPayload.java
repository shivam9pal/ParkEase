package com.parkease.notification.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEventPayload {

    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private BigDecimal amount;
    private String status;            // PAID, REFUNDED
    private String mode;              // CARD, UPI, WALLET, CASH
    private String transactionId;
    private String currency;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String description;
    private LocalDateTime createdAt;
}