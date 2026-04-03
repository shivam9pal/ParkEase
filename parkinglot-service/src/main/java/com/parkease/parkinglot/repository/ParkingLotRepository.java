package com.parkease.parkinglot.repository;

import com.parkease.parkinglot.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {

    Optional<ParkingLot> findByLotId(UUID lotId);

    List<ParkingLot> findByCity(String city);

    List<ParkingLot> findByManagerId(UUID managerId);

    List<ParkingLot> findByIsOpen(Boolean isOpen);

    List<ParkingLot> findByAvailableSpotsGreaterThan(int count);

    long countByCity(String city);

    void deleteByLotId(UUID lotId);

    // Haversine proximity search — native PostgreSQL query
    // Returns only approved & open lots, ordered by distance ASC
    @Query(value = """
            SELECT * FROM parking_lot
            WHERE is_approved = true AND is_open = true
            AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(latitude))
            )) < :radius
            ORDER BY (6371 * acos(
                cos(radians(:lat)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(latitude))
            )) ASC
            """, nativeQuery = true)
    List<ParkingLot> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radiusKm
    );

    // Keyword search across name, address, city
    List<ParkingLot> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrCityContainingIgnoreCase(
            String name, String address, String city
    );

    // Only approved lots — used for public-facing searches
    List<ParkingLot> findByIsApprovedTrue();

    // Pending approval lots — admin view
    List<ParkingLot> findByIsApprovedFalse();
}