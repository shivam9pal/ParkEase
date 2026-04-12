package com.parkease.booking.feign;

import com.parkease.booking.config.FeignConfig;
import com.parkease.booking.feign.dto.SpotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for spot-service (port 8083).
 *
 * Status transitions triggered by booking-service:
 *   AVAILABLE  → reserveSpot()  → RESERVED   (PRE_BOOKING creation)
 *   AVAILABLE  → occupySpot()   → OCCUPIED   (WALK_IN creation)
 *   RESERVED   → occupySpot()   → OCCUPIED   (PRE_BOOKING checkIn)
 *   OCCUPIED   → releaseSpot()  → AVAILABLE  (checkOut or cancel)
 *   RESERVED   → releaseSpot()  → AVAILABLE  (cancel from RESERVED)
 */
@FeignClient(
        name = "spot-service",
//        url = "${services.spot.url}",
        configuration = FeignConfig.class
)
public interface SpotServiceClient {

    /**
     * Fetch full spot details.
     * Used in createBooking() to:
     *   - Verify status == "AVAILABLE"
     *   - Validate vehicleType compatibility
     *   - Snapshot pricePerHour
     *   - Extract lotId
     */
    @GetMapping("/api/v1/spots/{spotId}")
    SpotResponse getSpotById(@PathVariable UUID spotId);

    /**
     * Transition: AVAILABLE → RESERVED
     * Called for PRE_BOOKING creation.
     * If this call succeeds and a later step fails,
     * releaseSpot() MUST be called to rollback.
     */
    @PutMapping("/api/v1/spots/{spotId}/reserve")
    SpotResponse reserveSpot(@PathVariable UUID spotId);

    /**
     * Transition: AVAILABLE or RESERVED → OCCUPIED
     * Called for WALK_IN creation and PRE_BOOKING checkIn().
     */
    @PutMapping("/api/v1/spots/{spotId}/occupy")
    SpotResponse occupySpot(@PathVariable UUID spotId);

    /**
     * Transition: RESERVED or OCCUPIED → AVAILABLE
     * Called on checkOut(), cancelBooking(), and autoExpireBookings().
     * This is the universal "undo" operation for all booking exits.
     */
    @PutMapping("/api/v1/spots/{spotId}/release")
    SpotResponse releaseSpot(@PathVariable UUID spotId);

    /**
     * Find available spots in a lot filtered by vehicle type.
     * Used to help drivers discover compatible spots (optional helper endpoint).
     * vehicleType passed as String — e.g., "TWO_WHEELER", "FOUR_WHEELER", "HEAVY"
     */
    @GetMapping("/api/v1/spots/lot/{lotId}/vehicle/{vehicleType}")
    List<SpotResponse> getAvailableSpotsByVehicleType(
            @PathVariable UUID lotId,
            @PathVariable String vehicleType
    );
}