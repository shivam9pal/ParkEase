package com.parkease.payment.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailDto {
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private String vehiclePlate;
    private String vehicleType;
    private String bookingType;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal pricePerHour;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;
}