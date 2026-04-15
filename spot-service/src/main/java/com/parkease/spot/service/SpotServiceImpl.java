package com.parkease.spot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkease.spot.dto.AddSpotRequest;
import com.parkease.spot.dto.BulkAddSpotRequest;
import com.parkease.spot.dto.SpotResponse;
import com.parkease.spot.dto.UpdateSpotRequest;
import com.parkease.spot.entity.ParkingSpot;
import com.parkease.spot.entity.SpotStatus;
import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import com.parkease.spot.repository.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final SpotRepository spotRepository;

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATION
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public SpotResponse addSpot(UUID lotId, AddSpotRequest request) {
        log.info("Adding spot [{}] to lot [{}]", request.getSpotNumber(), lotId);

        // Guard: spotNumber must be unique within the lot
        if (spotRepository.existsByLotIdAndSpotNumber(lotId, request.getSpotNumber())) {
            throw new IllegalArgumentException(
                    "Spot number '" + request.getSpotNumber() + "' already exists in lot " + lotId
            );
        }

        ParkingSpot spot = ParkingSpot.builder()
                .lotId(lotId)
                .spotNumber(request.getSpotNumber())
                .floor(request.getFloor())
                .spotType(request.getSpotType())
                .vehicleType(request.getVehicleType())
                .pricePerHour(request.getPricePerHour())
                .isHandicapped(request.isHandicapped())
                // EV spot type auto-enables charging; honour explicit flag otherwise
                .isEVCharging(
                        SpotType.EV.equals(request.getSpotType()) || request.isEVCharging()
                )
                .status(SpotStatus.AVAILABLE)
                .build();

        ParkingSpot saved = spotRepository.save(spot);
        log.info("Spot created with id [{}]", saved.getSpotId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<SpotResponse> addBulkSpots(UUID lotId, BulkAddSpotRequest request) {
        log.info("Bulk adding [{}] spots to lot [{}]", request.getCount(), lotId);

        // Resolve prefix: use provided prefix or fall back to spot type name
        String prefix = (request.getSpotNumberPrefix() != null
                && !request.getSpotNumberPrefix().isBlank())
                ? request.getSpotNumberPrefix()
                : request.getSpotType().name();

        boolean isEV = SpotType.EV.equals(request.getSpotType()) || request.isEVCharging();

        List<ParkingSpot> spotsToSave = new ArrayList<>();

        for (int i = 1; i <= request.getCount(); i++) {
            // Zero-pad to 2 digits: A-01, A-02, ..., A-10, A-11
            String spotNumber = prefix + "-" + String.format("%02d", i);

            // Skip if this spotNumber is already taken in the lot (graceful on re-runs)
            if (spotRepository.existsByLotIdAndSpotNumber(lotId, spotNumber)) {
                log.warn("Spot [{}] already exists in lot [{}] — skipping", spotNumber, lotId);
                continue;
            }

            ParkingSpot spot = ParkingSpot.builder()
                    .lotId(lotId)
                    .spotNumber(spotNumber)
                    .floor(request.getFloor())
                    .spotType(request.getSpotType())
                    .vehicleType(request.getVehicleType())
                    .pricePerHour(request.getPricePerHour())
                    .isHandicapped(request.isHandicapped())
                    .isEVCharging(isEV)
                    .status(SpotStatus.AVAILABLE)
                    .build();

            spotsToSave.add(spot);
        }

        List<ParkingSpot> saved = spotRepository.saveAll(spotsToSave);
        log.info("Bulk created [{}] spots in lot [{}]", saved.size(), lotId);
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public SpotResponse getSpotById(UUID spotId) {
        return toResponse(findSpotOrThrow(spotId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getSpotsByLot(UUID lotId) {
        return spotRepository.findByLotId(lotId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getAvailableSpots(UUID lotId) {
        return spotRepository.findByLotIdAndStatus(lotId, SpotStatus.AVAILABLE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getByTypeAndLot(UUID lotId, SpotType spotType) {
        return spotRepository.findByLotIdAndSpotType(lotId, spotType)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getByVehicleTypeAndLot(UUID lotId, VehicleType vehicleType) {
        return spotRepository.findByLotIdAndVehicleType(lotId, vehicleType)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getByFloorAndLot(UUID lotId, Integer floor) {
        return spotRepository.findByLotIdAndFloor(lotId, floor)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getEVSpots(UUID lotId) {
        return spotRepository.findByLotIdAndIsEVCharging(lotId, true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpotResponse> getHandicappedSpots(UUID lotId) {
        return spotRepository.findByLotIdAndIsHandicapped(lotId, true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countAvailable(UUID lotId) {
        return spotRepository.countByLotIdAndStatus(lotId, SpotStatus.AVAILABLE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATUS TRANSITIONS — STRICTLY ENFORCED
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * AVAILABLE → RESERVED Any other current state → 409 CONFLICT
     */
    @Override
    @Transactional
    public SpotResponse reserveSpot(UUID spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        switch (spot.getStatus()) {
            case AVAILABLE -> {
                spot.setStatus(SpotStatus.RESERVED);
                log.info("Spot [{}] AVAILABLE → RESERVED", spotId);
            }
            case RESERVED ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is already RESERVED — cannot reserve again"
                );
            case OCCUPIED ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is OCCUPIED — cannot reserve an occupied spot"
                );
        }

        return toResponse(spotRepository.save(spot));
    }

    /**
     * RESERVED → OCCUPIED (normal check-in) AVAILABLE → OCCUPIED (walk-in
     * direct check-in) Any other state → 409 CONFLICT
     */
    @Override
    @Transactional
    public SpotResponse occupySpot(UUID spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        switch (spot.getStatus()) {
            case AVAILABLE, RESERVED -> {
                log.info("Spot [{}] {} → OCCUPIED", spotId, spot.getStatus());
                spot.setStatus(SpotStatus.OCCUPIED);
            }
            case OCCUPIED ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is already OCCUPIED"
                );
        }

        return toResponse(spotRepository.save(spot));
    }

    /**
     * RESERVED, OCCUPIED → AVAILABLE (booking cancelled) OCCUPIED → AVAILABLE
     * (checkout) AVAILABLE stays → 409 CONFLICT (already free)
     */
    @Override
    @Transactional
    public SpotResponse releaseSpot(UUID spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        switch (spot.getStatus()) {
            case RESERVED, OCCUPIED -> {
                log.info("Spot [{}] {} → AVAILABLE", spotId, spot.getStatus());
                spot.setStatus(SpotStatus.AVAILABLE);
            }
            case AVAILABLE ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is already AVAILABLE — nothing to release"
                );
        }

        return toResponse(spotRepository.save(spot));
    }

    /**
     * AVAILABLE ↔ MAINTENANCE (toggle) Any other state → 409 CONFLICT
     */
    @Override
    @Transactional
    public SpotResponse toggleMaintenance(UUID spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        switch (spot.getStatus()) {
            case AVAILABLE -> {
                spot.setStatus(SpotStatus.MAINTENANCE);
                log.info("Spot [{}] AVAILABLE → MAINTENANCE", spotId);
            }
            case MAINTENANCE -> {
                spot.setStatus(SpotStatus.AVAILABLE);
                log.info("Spot [{}] MAINTENANCE → AVAILABLE", spotId);
            }
            case RESERVED ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is RESERVED — cannot put reserved spot under maintenance"
                );
            case OCCUPIED ->
                throw new IllegalStateException(
                        "Spot " + spotId + " is OCCUPIED — cannot put occupied spot under maintenance"
                );
        }

        return toResponse(spotRepository.save(spot));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE / DELETE
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Partial update — only non-null fields are applied. spotNumber and lotId
     * are immutable — never modified here.
     */
    @Override
    @Transactional
    public SpotResponse updateSpot(UUID spotId, UpdateSpotRequest request) {
        ParkingSpot spot = findSpotOrThrow(spotId);

        if (request.getSpotType() != null) {
            spot.setSpotType(request.getSpotType());
            // Re-enforce EV rule if type changed to EV
            if (SpotType.EV.equals(request.getSpotType())) {
                spot.setIsEVCharging(true);
            }
        }

        if (request.getVehicleType() != null) {
            spot.setVehicleType(request.getVehicleType());
        }

        if (request.getPricePerHour() != null) {
            spot.setPricePerHour(request.getPricePerHour());
        }

        if (request.getIsHandicapped() != null) {
            spot.setIsHandicapped(request.getIsHandicapped());
        }

        if (request.getIsEVCharging() != null) {
            spot.setIsEVCharging(request.getIsEVCharging());
        }

        if (request.getFloor() != null) {
            spot.setFloor(request.getFloor());
        }

        log.info("Spot [{}] updated", spotId);
        return toResponse(spotRepository.save(spot));
    }

    @Override
    @Transactional
    public void deleteSpot(UUID spotId) {
        ParkingSpot spot = findSpotOrThrow(spotId);
        spotRepository.delete(spot);
        log.info("Spot [{}] deleted", spotId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Central fetch helper — throws a named RuntimeException that
     * GlobalExceptionHandler maps to 404 NOT FOUND.
     */
    private ParkingSpot findSpotOrThrow(UUID spotId) {
        return spotRepository.findBySpotId(spotId)
                .orElseThrow(()
                        -> new RuntimeException("Spot not found with id: " + spotId)
                );
    }

    /**
     * Maps ParkingSpot entity → SpotResponse DTO. Entity is never exposed
     * directly to the API layer.
     */
    private SpotResponse toResponse(ParkingSpot spot) {
        return SpotResponse.builder()
                .spotId(spot.getSpotId())
                .lotId(spot.getLotId())
                .spotNumber(spot.getSpotNumber())
                .floor(spot.getFloor())
                .spotType(spot.getSpotType())
                .vehicleType(spot.getVehicleType())
                .status(spot.getStatus())
                .isHandicapped(spot.getIsHandicapped())
                .isEVCharging(spot.getIsEVCharging())
                .pricePerHour(spot.getPricePerHour())
                .createdAt(spot.getCreatedAt())
                .build();
    }
}
