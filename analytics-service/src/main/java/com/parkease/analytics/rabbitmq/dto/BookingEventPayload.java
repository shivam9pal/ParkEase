package com.parkease.analytics.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // Safe — booking-service may add fields later
public class BookingEventPayload {

    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private UUID vehicleId;
    private String vehiclePlate;
    private String vehicleType;         // TWO_WHEELER / FOUR_WHEELER / HEAVY
    private String bookingType;         // PRE_BOOKING / WALK_IN
    private String status;              // RESERVED / ACTIVE / COMPLETED / CANCELLED
    private BigDecimal totalAmount;     // null until COMPLETED
    private BigDecimal pricePerHour;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInTime;  // null for BOOKING_CREATED on PRE_BOOKING
    private LocalDateTime checkOutTime; // null until CHECKOUT
    private LocalDateTime createdAt;
}