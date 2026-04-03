package com.parkease.spot.dto;

import com.parkease.spot.entity.SpotStatus;
import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response DTO for a parking spot.
 *
 * <p>This is the ONLY object returned by spot-service APIs.
 * The ParkingSpot entity is never exposed directly.
 *
 * <p>pricePerHour is included here — booking-service reads it via
 * GET /api/v1/spots/{spotId} for fare calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotResponse {

    private UUID          spotId;
    private UUID          lotId;
    private String        spotNumber;
    private Integer       floor;
    private SpotType      spotType;
    private VehicleType   vehicleType;
    private SpotStatus    status;
    private Boolean       isHandicapped;
    private Boolean       isEVCharging;

    /**
     * BigDecimal — monetary precision required.
     * booking-service uses this for: fare = hours × pricePerHour
     */
    private BigDecimal    pricePerHour;

    private LocalDateTime createdAt;
}