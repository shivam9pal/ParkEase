package com.parkease.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateRazorpayOrderRequest {

    @NotNull(message = "bookingId is required.")
    private UUID bookingId;
}