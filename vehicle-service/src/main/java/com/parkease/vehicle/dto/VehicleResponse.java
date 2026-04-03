package com.parkease.vehicle.dto;

import com.parkease.vehicle.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO — entity is NEVER exposed directly.
 * booking-service deserializes this exact structure when calling
 * GET /api/v1/vehicles/{vehicleId} via RestTemplate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private UUID vehicleId;
    private UUID ownerId;
    private String licensePlate;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private Boolean isEV;
    private LocalDateTime registeredAt;
    private Boolean isActive;
}