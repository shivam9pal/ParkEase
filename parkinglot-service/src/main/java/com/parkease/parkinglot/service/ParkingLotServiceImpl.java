package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.CreateLotRequest;
import com.parkease.parkinglot.dto.LotResponse;
import com.parkease.parkinglot.dto.LotSummaryResponse;
import com.parkease.parkinglot.dto.UpdateLotRequest;
import com.parkease.parkinglot.entity.ParkingLot;
import com.parkease.parkinglot.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    // ─────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────
    @Override
    @Transactional
    public LotResponse createLot(UUID managerId, CreateLotRequest request) {
        ParkingLot lot = ParkingLot.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalSpots(request.getTotalSpots())
                .availableSpots(request.getTotalSpots()) // starts equal to totalSpots
                .managerId(managerId)
                .isOpen(true)
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .imageUrl(request.getImageUrl())
                .isApproved(false) // Admin must approve before lot is searchable
                .build();

        return toResponse(parkingLotRepository.save(lot));
    }

    // ─────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────
    @Override
    public LotResponse getLotById(UUID lotId) {
        return toResponse(findOrThrow(lotId));
    }

    @Override
    public List<LotResponse> getLotsByCity(String city) {
        // Public search — only approved lots visible
        return parkingLotRepository.findByIsApprovedTrue()
                .stream()
                .filter(lot -> lot.getCity().equalsIgnoreCase(city))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        // Haversine query filters is_approved=true AND is_open=true natively
        return parkingLotRepository.findNearby(lat, lng, radiusKm)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotResponse> getLotsByManager(UUID managerId) {
        return parkingLotRepository.findByManagerId(managerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotResponse> searchLots(String keyword) {
        // Public keyword search — filter approved lots only after query
        return parkingLotRepository
                .findByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrCityContainingIgnoreCase(
                        keyword, keyword, keyword)
                .stream()
                .filter(ParkingLot::getIsApproved)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotResponse> getAllLots() {
        // Admin view — approved + pending
        return parkingLotRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotResponse> getPendingLots() {
        return parkingLotRepository.findByIsApprovedFalse()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LotSummaryResponse> getAllApprovedLots() {
        // Public view — only approved lots visible
        return parkingLotRepository.findByIsApprovedTrue()
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────
    @Override
    @Transactional
    public LotResponse updateLot(UUID lotId, UUID requesterId, UpdateLotRequest request) {
        ParkingLot lot = findOrThrow(lotId);
        enforceOwnerAccess(lot, requesterId);

        if (request.getName() != null) {
            lot.setName(request.getName());
        }
        if (request.getAddress() != null) {
            lot.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            lot.setCity(request.getCity());
        }
        if (request.getLatitude() != null) {
            lot.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            lot.setLongitude(request.getLongitude());
        }
        if (request.getOpenTime() != null) {
            lot.setOpenTime(request.getOpenTime());
        }
        if (request.getCloseTime() != null) {
            lot.setCloseTime(request.getCloseTime());
        }
        if (request.getImageUrl() != null) {
            lot.setImageUrl(request.getImageUrl());
        }

        return toResponse(parkingLotRepository.save(lot));
    }

    @Override
    @Transactional
    public LotResponse toggleOpen(UUID lotId, UUID managerId) {
        ParkingLot lot = findOrThrow(lotId);
        enforceOwnerAccess(lot, managerId);
        lot.setIsOpen(!lot.getIsOpen());
        return toResponse(parkingLotRepository.save(lot));
    }

    @Override
    @Transactional
    public LotResponse approveLot(UUID lotId) {
        ParkingLot lot = findOrThrow(lotId);
        lot.setIsApproved(true);
        return toResponse(parkingLotRepository.save(lot));
    }

    // ─────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteLot(UUID lotId, UUID requesterId, String requesterRole) {
        ParkingLot lot = findOrThrow(lotId);
        if (!"ADMIN".equals(requesterRole)) {
            enforceOwnerAccess(lot, requesterId);
        }
        parkingLotRepository.deleteByLotId(lotId);
    }

    // ─────────────────────────────────────────────────
    // ATOMIC SPOT COUNTER — called by booking-service
    // ─────────────────────────────────────────────────
    @Override
    @Transactional
    public void decrementAvailable(UUID lotId) {
        ParkingLot lot = findOrThrow(lotId);
        if (lot.getAvailableSpots() <= 0) {
            throw new IllegalStateException("No available spots in lot: " + lotId);
        }
        lot.setAvailableSpots(lot.getAvailableSpots() - 1);
        parkingLotRepository.save(lot);
    }

    @Override
    @Transactional
    public void incrementAvailable(UUID lotId) {
        ParkingLot lot = findOrThrow(lotId);
        if (lot.getAvailableSpots() >= lot.getTotalSpots()) {
            throw new IllegalStateException("Available spots already at maximum for lot: " + lotId);
        }
        lot.setAvailableSpots(lot.getAvailableSpots() + 1);
        parkingLotRepository.save(lot);
    }

    // ─────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────
    private ParkingLot findOrThrow(UUID lotId) {
        return parkingLotRepository.findByLotId(lotId)
                .orElseThrow(() -> new RuntimeException("Parking lot not found with id: " + lotId));
    }

    /**
     * Throws 403-equivalent exception if caller is not the lot's manager. Used
     * by update / toggle / delete operations.
     */
    private void enforceOwnerAccess(ParkingLot lot, UUID requesterId) {
        if (!lot.getManagerId().equals(requesterId)) {
            throw new SecurityException("Access denied: you do not own this parking lot");
        }
    }

    private LotResponse toResponse(ParkingLot lot) {
        return LotResponse.builder()
                .lotId(lot.getLotId())
                .name(lot.getName())
                .address(lot.getAddress())
                .city(lot.getCity())
                .latitude(lot.getLatitude())
                .longitude(lot.getLongitude())
                .totalSpots(lot.getTotalSpots())
                .availableSpots(lot.getAvailableSpots())
                .managerId(lot.getManagerId())
                .isOpen(lot.getIsOpen())
                .openTime(lot.getOpenTime())
                .closeTime(lot.getCloseTime())
                .imageUrl(lot.getImageUrl())
                .isApproved(lot.getIsApproved())
                .createdAt(lot.getCreatedAt())
                .build();
    }

    private LotSummaryResponse toSummaryResponse(ParkingLot lot) {
        return LotSummaryResponse.builder()
                .lotId(lot.getLotId())
                .name(lot.getName())
                .address(lot.getAddress())
                .managerId(lot.getManagerId())
                .totalSpots(lot.getTotalSpots())
                .availableSpots(lot.getAvailableSpots())
                .isApproved(lot.getIsApproved())
                .createdAt(lot.getCreatedAt())
                .build();
    }
}
