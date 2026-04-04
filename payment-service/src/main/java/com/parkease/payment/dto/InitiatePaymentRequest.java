package com.parkease.payment.dto;

import com.parkease.payment.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "bookingId is required.")
    private UUID bookingId;

    @NotNull(message = "Payment mode is required. Accepted: CARD, UPI, WALLET, CASH")
    private PaymentMode mode;
}