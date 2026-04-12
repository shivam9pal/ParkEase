package com.parkease.spot.service;

import java.util.List;
import java.util.UUID;

import com.parkease.spot.dto.AddSpotRequest;
import com.parkease.spot.dto.BulkAddSpotRequest;
import com.parkease.spot.dto.SpotResponse;
import com.parkease.spot.dto.UpdateSpotRequest;
import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;

/**
 * Business contract for spot-service.
 *
 * <p>
 * All write methods are @Transactional in the implementation. Status transition
 * methods (reserve/occupy/release) enforce strict state-machine rules — invalid
 * transitions throw IllegalStateException → 409.
 */
public interface SpotService {

    // ─────────────────────── Creation ──────────────────────────────────────
    /**
     * Add a single parking spot to the given lot.
     */
    SpotResponse addSpot(UUID lotId, AddSpotRequest request);

    /**
     * Bulk-create spots using auto-numbering (prefix + zero-padded counter).
     */
    List<SpotResponse> addBulkSpots(UUID lotId, BulkAddSpotRequest request);

    // ─────────────────────── Read ───────────────────────────────────────────
    /**
     * Fetch full spot details by spotId — used by booking-service for
     * pricePerHour.
     */
    SpotResponse getSpotById(UUID spotId);

    /**
     * All spots in a lot — used by analytics-service.
     */
    List<SpotResponse> getSpotsByLot(UUID lotId);

    /**
     * Only AVAILABLE spots — primary listing for drivers.
     */
    List<SpotResponse> getAvailableSpots(UUID lotId);

    /**
     * Filter by spot type within a lot.
     */
    List<SpotResponse> getByTypeAndLot(UUID lotId, SpotType spotType);

    /**
     * Filter by vehicle type within a lot. Returns ALL statuses —
     * booking-service uses this then checks AVAILABLE.
     */
    List<SpotResponse> getByVehicleTypeAndLot(UUID lotId, VehicleType vehicleType);

    /**
     * All spots on a specific floor — supports floor-plan frontend view.
     */
    List<SpotResponse> getByFloorAndLot(UUID lotId, Integer floor);

    /**
     * EV charging spots in the lot.
     */
    List<SpotResponse> getEVSpots(UUID lotId);

    /**
     * Handicapped accessible spots in the lot.
     */
    List<SpotResponse> getHandicappedSpots(UUID lotId);

    // ─────────────────────── Status Transitions ────────────────────────────
    /**
     * AVAILABLE → OCCUPIED (walk-in) RESERVED → OCCUPIED (normal check-in) Any
     * other state → throws IllegalStateException (409)
     */
    SpotResponse occupySpot(UUID spotId);

    /**
     * AVAILABLE → RESERVED (booking created) Any other state → throws
     * IllegalStateException (409)
     */
    SpotResponse reserveSpot(UUID spotId);

    /**
     * RESERVED → AVAILABLE (booking cancelled) OCCUPIED → AVAILABLE (checkout)
     * Any other state → throws IllegalStateException (409)
     */
    SpotResponse releaseSpot(UUID spotId);

    /**
     * AVAILABLE → MAINTENANCE (maintenance starts) MAINTENANCE → AVAILABLE
     * (maintenance ends) Any other state → throws IllegalStateException (409)
     */
    SpotResponse toggleMaintenance(UUID spotId);

    // ─────────────────────── Update / Delete ────────────────────────────────
    /**
     * Partial update of mutable spot fields. spotNumber and lotId are IMMUTABLE
     * — never changed here.
     */
    SpotResponse updateSpot(UUID spotId, UpdateSpotRequest request);

    /**
     * Hard-delete spot record. Only MANAGER/ADMIN.
     */
    void deleteSpot(UUID spotId);

    // ─────────────────────── Aggregates ─────────────────────────────────────
    /**
     * Count of AVAILABLE spots — used by analytics-service.
     */
    long countAvailable(UUID lotId);
}
