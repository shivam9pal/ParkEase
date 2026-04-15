package com.parkease.payment.dto;

import com.parkease.payment.enums.PaymentMode;
import com.parkease.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for payment summary listing (admin view) Contains essential payment
 * fields for dashboard/admin pages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryResponse {

    private UUID paymentId;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMode mode;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  // Maps to paidAt or refundedAt
}
