package com.parkease.parkinglot.service;

import com.parkease.parkinglot.dto.CreateLotRequest;
import com.parkease.parkinglot.dto.LotResponse;
import com.parkease.parkinglot.dto.UpdateLotRequest;

import java.util.List;
import java.util.UUID;

public interface ParkingLotService {

    LotResponse createLot(UUID managerId, CreateLotRequest request);

    LotResponse getLotById(UUID lotId);

    List<LotResponse> getLotsByCity(String city);

    List<LotResponse> getNearbyLots(double lat, double lng, double radiusKm);

    List<LotResponse> getLotsByManager(UUID managerId);

    List<LotResponse> searchLots(String keyword);

    List<LotResponse> getAllLots();

    List<LotResponse> getPendingLots();

    LotResponse updateLot(UUID lotId, UUID managerId, UpdateLotRequest request);

    LotResponse toggleOpen(UUID lotId, UUID managerId);

    LotResponse approveLot(UUID lotId);

    void deleteLot(UUID lotId, UUID requesterId, String requesterRole);

    void decrementAvailable(UUID lotId);   // called by booking-service

    void incrementAvailable(UUID lotId);   // called by booking-service
}