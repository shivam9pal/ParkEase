package com.parkease.vehicle.service;

import com.parkease.vehicle.dto.RegisterVehicleRequest;
import com.parkease.vehicle.dto.UpdateVehicleRequest;
import com.parkease.vehicle.dto.VehicleResponse;
import com.parkease.vehicle.entity.VehicleType;

import java.util.List;
import java.util.UUID;

/**
 * Business contract for vehicle-service.
 * All implementations must honour these method signatures exactly —
 * booking-service depends on getVehicleById() and isEVVehicle() for spot assignment.
 */
public interface VehicleService {

    /**
     * Register a new vehicle for the authenticated driver.
     * ownerId is extracted from JWT — never from request body.
     */
    VehicleResponse registerVehicle(UUID ownerId, RegisterVehicleRequest request);

    /**
     * Fetch a vehicle by its primary UUID key.
     * Called by booking-service via RestTemplate.
     */
    VehicleResponse getVehicleById(UUID vehicleId);

    /**
     * Fetch all active vehicles owned by a given driver.
     */
    List<VehicleResponse> getVehiclesByOwner(UUID ownerId);

    /**
     * Lookup a vehicle by its license plate (global — not per-owner).
     */
    VehicleResponse getByLicensePlate(String licensePlate);

    /**
     * Partially update a vehicle's details.
     * Only fields present (non-null) in the request are applied.
     */
    VehicleResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request);

    /**
     * Soft delete — sets isActive = false. No hard delete ever.
     */
    void deleteVehicle(UUID vehicleId);

    /**
     * Returns the VehicleType enum for a given vehicle.
     * Used by booking-service to validate spot compatibility.
     */
    VehicleType getVehicleType(UUID vehicleId);

    /**
     * Returns true if the vehicle is electric (EV).
     * booking-service calls GET /{vehicleId}/isEV and maps this to Boolean.
     */
    boolean isEVVehicle(UUID vehicleId);

    /**
     * Admin-only: return all vehicles in the system regardless of owner.
     */
    List<VehicleResponse> getAllVehicles();
}