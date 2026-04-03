package com.parkease.vehicle.service;

import com.parkease.vehicle.dto.RegisterVehicleRequest;
import com.parkease.vehicle.dto.UpdateVehicleRequest;
import com.parkease.vehicle.dto.VehicleResponse;
import com.parkease.vehicle.entity.Vehicle;
import com.parkease.vehicle.entity.VehicleType;
import com.parkease.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public VehicleResponse registerVehicle(UUID ownerId, RegisterVehicleRequest request) {
        log.info("Registering vehicle for ownerId={}, plate={}", ownerId, request.getLicensePlate());

        // ── Business Rule: license plate is unique PER owner ──
        // Same plate CAN belong to two different drivers (different cars).
        vehicleRepository.findByOwnerIdAndLicensePlate(ownerId, request.getLicensePlate())
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "License plate '" + request.getLicensePlate() +
                                    "' is already registered to your account"
                    );
                });

        Vehicle vehicle = Vehicle.builder()
                .ownerId(ownerId)
                .licensePlate(request.getLicensePlate().toUpperCase().trim())
                .make(request.getMake())
                .model(request.getModel())
                .color(request.getColor())
                .vehicleType(request.getVehicleType())
                .isEV(request.isEV())
                .isActive(true)
                .build();
        // registeredAt is set via @PrePersist in the entity

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle registered successfully: vehicleId={}", saved.getVehicleId());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID vehicleId) {
        log.debug("Fetching vehicle by id={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        return toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByOwner(UUID ownerId) {
        log.debug("Fetching all vehicles for ownerId={}", ownerId);

        return vehicleRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getByLicensePlate(String licensePlate) {
        log.debug("Fetching vehicle by plate={}", licensePlate);

        Vehicle vehicle = vehicleRepository.findByLicensePlate(licensePlate.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException(
                        "Vehicle not found with license plate: " + licensePlate
                ));

        return toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        log.debug("Admin: fetching all vehicles");

        return vehicleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request) {
        log.info("Updating vehicle vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        // ── Partial update — only apply non-null fields ──
        if (request.getMake() != null && !request.getMake().isBlank()) {
            vehicle.setMake(request.getMake());
        }
        if (request.getModel() != null && !request.getModel().isBlank()) {
            vehicle.setModel(request.getModel());
        }
        if (request.getColor() != null && !request.getColor().isBlank()) {
            vehicle.setColor(request.getColor());
        }
        if (request.getVehicleType() != null) {
            vehicle.setVehicleType(request.getVehicleType());
        }
        if (request.getIsEV() != null) {
            vehicle.setIsEV(request.getIsEV());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: vehicleId={}", updated.getVehicleId());

        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteVehicle(UUID vehicleId) {
        log.info("Soft-deleting vehicle vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        // ── SOFT DELETE — never hard-delete, booking history depends on this ──
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);

        log.info("Vehicle soft-deleted: vehicleId={}", vehicleId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TYPE & EV QUERIES (used by booking-service via RestTemplate)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VehicleType getVehicleType(UUID vehicleId) {
        log.debug("Getting vehicleType for vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        return vehicle.getVehicleType();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEVVehicle(UUID vehicleId) {
        log.debug("Checking EV status for vehicleId={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        return Boolean.TRUE.equals(vehicle.getIsEV());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE MAPPER — Entity → DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps Vehicle entity to VehicleResponse DTO.
     * Entity is NEVER returned directly from any endpoint.
     */
    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .ownerId(vehicle.getOwnerId())
                .licensePlate(vehicle.getLicensePlate())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .isEV(vehicle.getIsEV())
                .registeredAt(vehicle.getRegisteredAt())
                .isActive(vehicle.getIsActive())
                .build();
    }
}