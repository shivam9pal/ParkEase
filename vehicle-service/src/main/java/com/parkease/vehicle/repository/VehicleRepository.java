package com.parkease.vehicle.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkease.vehicle.entity.Vehicle;
import com.parkease.vehicle.entity.VehicleType;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    /**
     * Get all vehicles owned by a specific driver. Used by: GET
     * /api/v1/vehicles/owner/{ownerId}
     */
    List<Vehicle> findByOwnerId(UUID ownerId);

    /**
     * Get all ACTIVE vehicles owned by a specific driver (excludes
     * soft-deleted). Used by: GET /api/v1/vehicles/owner/{ownerId} for driver
     * dashboard
     */
    List<Vehicle> findByOwnerIdAndIsActiveTrue(UUID ownerId);

    /**
     * Find a vehicle by its license plate (global lookup). Used by: GET
     * /api/v1/vehicles/plate/{licensePlate}
     */
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    /**
     * Find a vehicle by its primary key UUID. Used by: GET
     * /api/v1/vehicles/{vehicleId}
     */
    Optional<Vehicle> findByVehicleId(UUID vehicleId);

    /**
     * Get all vehicles of a specific type (2W / 4W / Heavy). Used by: analytics
     * and admin filters.
     */
    List<Vehicle> findByVehicleType(VehicleType vehicleType);

    /**
     * Get all EV or non-EV vehicles. Used by: admin / analytics queries.
     */
    List<Vehicle> findByIsEV(Boolean isEV);

    /**
     * Check if a license plate already exists in the system (global). Used as a
     * quick pre-check before the per-owner duplicate check.
     */
    boolean existsByLicensePlate(String licensePlate);

    /**
     * Soft-delete support: find by UUID then mark isActive = false. Hard delete
     * is NOT used. This method is available if needed.
     */
    void deleteByVehicleId(UUID vehicleId);

    /**
     * ⭐ Core business rule: license plate is unique PER owner (ACTIVE only).
     * Used in registerVehicle() to prevent same driver registering the same
     * plate twice. Soft-deleted vehicles are ignored — allows re-registration
     * of same plate. Different drivers CAN share plates.
     */
    Optional<Vehicle> findByOwnerIdAndLicensePlateAndIsActiveTrue(UUID ownerId, String licensePlate);
}
