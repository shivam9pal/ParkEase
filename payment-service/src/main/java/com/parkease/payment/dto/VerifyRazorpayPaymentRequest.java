package com.parkease.payment.dto;

import com.parkease.payment.enums.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class VerifyRazorpayPaymentRequest {

    @NotNull(message = "paymentId is required.")
    private UUID paymentId;

    @NotBlank(message = "razorpayOrderId is required.")
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required.")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required.")
    private String razorpaySignature;

    @NotNull(message = "Payment mode is required. Accepted: CARD, UPI, WALLET")
    private PaymentMode mode;           // CARD | UPI | WALLET (not CASH)
}