package com.parkease.booking.feign;

import com.parkease.booking.config.FeignConfig;
import com.parkease.booking.feign.dto.VehicleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for vehicle-service (port 8086).
 *
 * Used in createBooking() to:
 *   1. Verify vehicle exists and belongs to the requesting user
 *   2. Confirm vehicle is active (not soft-deleted)
 *   3. Fetch vehicleType for spot compatibility check
 *   4. Fetch licensePlate for denormalized storage in Booking
 *   5. Fetch isEV for EV charging spot validation
 */
@FeignClient(
        name = "vehicle-service",
        configuration = FeignConfig.class
)
public interface VehicleServiceClient {

    /**
     * Primary vehicle lookup by UUID.
     * Used in createBooking() — vehicleId comes from CreateBookingRequest.
     */
    @GetMapping("/api/v1/vehicles/{vehicleId}")
    VehicleResponse getVehicleById(@PathVariable UUID vehicleId);

    /**
     * Vehicle lookup by license plate.
     * Useful for walk-in scenarios where plate is known but vehicleId is not.
     * Also used for manual lookup by managers.
     */
    @GetMapping("/api/v1/vehicles/plate/{licensePlate}")
    VehicleResponse getVehicleByPlate(@PathVariable String licensePlate);
}