package com.parkease.payment.rabbitmq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingEventPayload {
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private UUID vehicleId;
    private String vehiclePlate;
    private String vehicleType;
    private String bookingType;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal pricePerHour;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;
}