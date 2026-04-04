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
@JsonIgnoreProperties(ignoreUnknown = true)   // Safe against booking-service adding new fields
public class BookingEventPayload {

    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private UUID vehicleId;
    private String vehiclePlate;
    private String vehicleType;       // TWO_WHEELER, FOUR_WHEELER, HEAVY
    private String bookingType;       // PRE_BOOKING, WALK_IN
    private String status;            // RESERVED, ACTIVE, COMPLETED, CANCELLED
    private BigDecimal totalAmount;   // null until COMPLETED
    private BigDecimal pricePerHour;
    private LocalDateTime startTime;
    private LocalDateTime endTime;    // Updated value for BOOKING_EXTENDED events
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;
}