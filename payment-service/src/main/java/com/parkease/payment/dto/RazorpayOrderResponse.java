package com.parkease.payment.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class RazorpayOrderResponse {
    private String razorpayOrderId;    // order_XXXXXXXXXXXXXXX
    private String razorpayKeyId;      // sent to frontend to open Razorpay Checkout
    private BigDecimal amount;         // display amount in INR
    private long amountInPaise;        // amount × 100
    private String currency;
    private UUID paymentId;            // your internal payment ID
    private UUID bookingId;
}