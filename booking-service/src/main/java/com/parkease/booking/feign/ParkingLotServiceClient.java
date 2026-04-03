package com.parkease.booking.feign;

import com.parkease.booking.config.FeignConfig;
import com.parkease.booking.feign.dto.LotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

/**
 * Feign client for parkinglot-service (port 8082).
 *
 * Counter management rules:
 *   decrementAvailableSpots() → called when a spot is taken (booking created)
 *   incrementAvailableSpots() → called when a spot is freed (checkout or cancel)
 *
 * ROLLBACK RULE:
 *   If DB save fails after decrement → call incrementAvailableSpots() to undo.
 */
@FeignClient(
        name = "parkinglot-service",
        url = "${services.parkinglot.url}",
        configuration = FeignConfig.class
)
public interface ParkingLotServiceClient {

    /**
     * Fetch lot details — used to verify lot exists and is open/approved
     * before accepting a booking.
     */
    @GetMapping("/api/v1/lots/{lotId}")
    LotResponse getLotById(@PathVariable UUID lotId);

    /**
     * Decrement availableSpots counter by 1.
     * Called after reserveSpot() or occupySpot() succeeds in createBooking().
     *
     * If this call fails → releaseSpot() on spot-service to rollback.
     */
    @PutMapping("/api/v1/lots/{lotId}/decrement")
    LotResponse decrementAvailableSpots(@PathVariable UUID lotId);

    /**
     * Increment availableSpots counter by 1.
     * Called on checkOut(), cancelBooking(), and autoExpireBookings().
     */
    @PutMapping("/api/v1/lots/{lotId}/increment")
    LotResponse incrementAvailableSpots(@PathVariable UUID lotId);
}