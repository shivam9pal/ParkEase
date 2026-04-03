package com.parkease.vehicle.dto;

import com.parkease.vehicle.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVehicleRequest {

    @NotBlank(message = "License plate is required")
    @Size(min = 2, max = 20, message = "License plate must be between 2 and 20 characters")
    @Pattern(
            regexp = "^[A-Z0-9 \\-]+$",
            message = "License plate must contain only uppercase letters, digits, spaces, or hyphens"
    )
    private String licensePlate;

    @NotBlank(message = "Make is required (e.g., Toyota, Honda)")
    @Size(max = 50, message = "Make must not exceed 50 characters")
    private String make;

    @NotBlank(message = "Model is required (e.g., Innova, City)")
    @Size(max = 50, message = "Model must not exceed 50 characters")
    private String model;

    @Size(max = 30, message = "Color must not exceed 30 characters")
    private String color;

    @NotNull(message = "Vehicle type is required: TWO_WHEELER, FOUR_WHEELER, or HEAVY")
    private VehicleType vehicleType;

    /**
     * EV flag — defaults to false if not provided.
     * booking-service uses this to match EV vehicles to EV-charging spots.
     */
    private boolean isEV = false;
}