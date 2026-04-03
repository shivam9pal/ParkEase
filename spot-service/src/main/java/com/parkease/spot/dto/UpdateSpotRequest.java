package com.parkease.spot.dto;

import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for partially updating an existing parking spot.
 *
 * <p>All fields are optional — only non-null fields are applied.
 * IMMUTABLE fields (spotNumber, lotId) are intentionally absent from this DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSpotRequest {

    /** Optional — update spot category */
    private SpotType spotType;

    /** Optional — update vehicle compatibility */
    private VehicleType vehicleType;

    /** Optional — update hourly rate */
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    /** Optional — toggle handicapped accessibility */
    private Boolean isHandicapped;

    /** Optional — toggle EV charging availability */
    private Boolean isEVCharging;

    /** Optional — reassign to a different floor within the same lot */
    private Integer floor;

    // ── spotNumber is IMMUTABLE after creation — NOT included here ────────────
    // ── lotId      is IMMUTABLE after creation — NOT included here ────────────
}