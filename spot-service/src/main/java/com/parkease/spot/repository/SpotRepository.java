package com.parkease.spot.repository;

import com.parkease.spot.entity.ParkingSpot;
import com.parkease.spot.entity.SpotStatus;
import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpotRepository extends JpaRepository<ParkingSpot, UUID> {

    // ─────────────────────── Primary Lookups ───────────────────────────────

    Optional<ParkingSpot> findBySpotId(UUID spotId);

    // ─────────────────────── Lot-Level Queries ─────────────────────────────

    List<ParkingSpot> findByLotId(UUID lotId);

    List<ParkingSpot> findByLotIdAndStatus(UUID lotId, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotType(UUID lotId, SpotType spotType);

    List<ParkingSpot> findByLotIdAndVehicleType(UUID lotId, VehicleType vehicleType);

    // ─────────────────────── Combined Filters ──────────────────────────────

    List<ParkingSpot> findByLotIdAndVehicleTypeAndStatus(
            UUID lotId, VehicleType vehicleType, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotTypeAndStatus(
            UUID lotId, SpotType spotType, SpotStatus status);

    List<ParkingSpot> findByLotIdAndFloor(UUID lotId, Integer floor);

    List<ParkingSpot> findByLotIdAndIsHandicapped(UUID lotId, Boolean isHandicapped);

    List<ParkingSpot> findByLotIdAndIsEVCharging(UUID lotId, Boolean isEVCharging);

    // ─────────────────────── Global / EV Queries ───────────────────────────

    List<ParkingSpot> findByIsEVCharging(Boolean isEVCharging);

    // ─────────────────────── Existence / Count Checks ──────────────────────

    boolean existsByLotIdAndSpotNumber(UUID lotId, String spotNumber);

    long countByLotIdAndStatus(UUID lotId, SpotStatus status);

    // ─────────────────────── Delete ────────────────────────────────────────

    void deleteBySpotId(UUID spotId);
}