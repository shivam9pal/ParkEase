package com.parkease.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentStatusResponse {
    private UUID paymentId;
    private UUID bookingId;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}