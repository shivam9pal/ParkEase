package com.parkease.spot.dto;

import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating multiple spots in one batch.
 *
 * <p>Auto-numbering rule:
 * If spotNumberPrefix = "A" and count = 5 → generates A-01, A-02, A-03, A-04, A-05
 * If spotNumberPrefix is null/blank → uses spotType.name() as prefix:
 *   COMPACT-01, COMPACT-02 ...
 *
 * <p>lotId is provided separately as a path variable on the endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddSpotRequest {

    @NotNull(message = "Count of spots to create is required")
    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;

    @NotNull(message = "Spot type is required for bulk creation")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required for bulk creation")
    private VehicleType vehicleType;

    @NotNull(message = "Floor is required for bulk creation")
    private Integer floor;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    /**
     * Optional prefix for auto-generated spot numbers.
     * e.g. "A" → A-01, A-02 ...
     * If null or blank, spotType.name() is used as prefix.
     */
    private String spotNumberPrefix;

    /** All spots in batch share this handicapped flag */
    private boolean isHandicapped = false;

    /**
     * All spots in batch share this EV flag.
     * Auto-forced to true if spotType = EV (enforced in service layer).
     */
    private boolean isEVCharging = false;
}