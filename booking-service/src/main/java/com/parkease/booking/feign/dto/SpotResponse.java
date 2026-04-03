package com.parkease.booking.feign.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirror of spot-service SpotResponse.
 * Used only to deserialize Feign responses — NOT a canonical entity.
 *
 * Critical fields consumed by booking-service:
 *   - status       → must be "AVAILABLE" before booking
 *   - vehicleType  → must match vehicle's vehicleType
 *   - pricePerHour → snapshotted into Booking.pricePerHour at creation
 *   - lotId        → stored in Booking.lotId (driver doesn't supply this directly)
 *   - isEVCharging → validated if vehicle.isEV == true
 */
@Data
public class SpotResponse {

    private UUID spotId;
    private UUID lotId;
    private String spotNumber;
    private Integer floor;

    // "STANDARD", "COMPACT", "LARGE", "EV", "HANDICAPPED" — spot-service SpotType
    private String spotType;

    // "TWO_WHEELER", "FOUR_WHEELER", "HEAVY" — must match vehicle's vehicleType
    private String vehicleType;

    // "AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE" — spot-service SpotStatus
    private String status;

    private Boolean isHandicapped;
    private Boolean isEVCharging;

    // Snapshotted into Booking.pricePerHour at booking creation time
    // BigDecimal mandatory — never float/double for monetary values
    private BigDecimal pricePerHour;
}