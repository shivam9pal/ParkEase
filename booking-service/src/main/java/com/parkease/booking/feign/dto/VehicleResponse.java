package com.parkease.booking.feign.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Mirror of vehicle-service VehicleResponse.
 * Used to:
 *   1. Verify vehicle.ownerId == JWT userId (ownership check)
 *   2. Denormalize vehiclePlate and vehicleType into Booking entity
 *   3. Check isEV for EV charging spot validation
 *   4. Check isActive — inactive (soft-deleted) vehicles cannot be booked
 */
@Data
public class VehicleResponse {

    private UUID vehicleId;

    // ownerId from vehicle-service = userId in auth-service — cross-service UUID reference
    private UUID ownerId;

    private String licensePlate;

    // "TWO_WHEELER", "FOUR_WHEELER", "HEAVY" — must match spot's vehicleType
    private String vehicleType;

    private Boolean isEV;

    // Soft-delete flag — must be true to allow booking
    private Boolean isActive;
}