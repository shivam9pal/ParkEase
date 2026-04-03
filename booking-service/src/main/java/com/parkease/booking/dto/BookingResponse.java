package com.parkease.booking.dto;

import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.entity.BookingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Universal response DTO — returned by every booking endpoint
 * and also used as the RabbitMQ event payload.
 *
 * Null field rules:
 *   checkInTime  → null for RESERVED PRE_BOOKING (set at checkIn)
 *   checkOutTime → null until checkOut
 *   totalAmount  → null until checkOut — NEVER return 0 for in-progress bookings
 *
 * vehicleType returned as String (not enum) so API consumers
 * don't need to import booking-service's VehicleType enum.
 */
@Data
@Builder
public class BookingResponse {

    private UUID bookingId;

    // Driver who owns this booking
    private UUID userId;

    // Parking lot — derived from spot at creation time
    private UUID lotId;

    // Specific spot reserved/occupied
    private UUID spotId;

    // Vehicle used for this booking
    private UUID vehicleId;

    // Denormalized — stored at creation, no cross-service call needed
    private String vehiclePlate;

    // "TWO_WHEELER" / "FOUR_WHEELER" / "HEAVY" — as String for API consumers
    private String vehicleType;

    private BookingType bookingType;
    private BookingStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // null for RESERVED PRE_BOOKING — populated at checkIn()
    private LocalDateTime checkInTime;

    // null until checkOut() is called
    private LocalDateTime checkOutTime;

    // Snapshotted at booking creation — used for fare display and calculation
    private BigDecimal pricePerHour;

    // null until checkOut() — final computed fare (min 1 hour)
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;
}