package com.parkease.spot.dto;

import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for adding a single parking spot to a lot.
 * lotId is taken from the path variable — NOT from this body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSpotRequest {

    @NotBlank(message = "Spot number is required (e.g. A-01, B-12)")
    private String spotNumber;

    @NotNull(message = "Floor is required (0 = Ground, 1 = First, -1 = Basement 1)")
    private Integer floor;

    @NotNull(message = "Spot type is required: COMPACT, STANDARD, LARGE, MOTORBIKE, EV")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required: TWO_WHEELER, FOUR_WHEELER, HEAVY")
    private VehicleType vehicleType;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.01", inclusive = false, message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    /** Handicapped accessible — defaults to false if not provided */
    private boolean isHandicapped = false;

    /**
     * EV charging available — defaults to false.
     * Automatically set to true if spotType = EV (enforced in service layer).
     */
    private boolean isEVCharging = false;
}