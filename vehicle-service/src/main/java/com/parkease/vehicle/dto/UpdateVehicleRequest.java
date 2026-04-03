package com.parkease.vehicle.dto;

import com.parkease.vehicle.entity.VehicleType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All fields are optional — only non-null fields are applied during update.
 * licensePlate and ownerId are intentionally excluded — they cannot be changed after registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {

    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    private VehicleType vehicleType;

    // Wrapped Boolean so null = "not provided, don't change"
    private Boolean isEV;
}