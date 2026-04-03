package com.parkease.parkinglot.resource;

import com.parkease.parkinglot.dto.CreateLotRequest;
import com.parkease.parkinglot.dto.LotResponse;
import com.parkease.parkinglot.dto.UpdateLotRequest;
import com.parkease.parkinglot.security.JwtUtil;
import com.parkease.parkinglot.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
@Tag(name = "Parking Lot", description = "Parking Lot Management APIs")
public class ParkingLotResource {

    private final ParkingLotService parkingLotService;
    private final JwtUtil jwtUtil;

    // ─────────────────────────────────────────────────
    // HELPER — extract claims from JWT header
    // ─────────────────────────────────────────────────

    private UUID extractUserId(String authHeader) {
        return jwtUtil.extractUserId(authHeader.substring(7));
    }

    private String extractRole(String authHeader) {
        return jwtUtil.extractRole(authHeader.substring(7));
    }

    // ─────────────────────────────────────────────────
    // POST /api/v1/lots — MANAGER creates a lot
    // ─────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Register a new parking lot (MANAGER only)")
    public ResponseEntity<LotResponse> createLot(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateLotRequest request) {

        UUID managerId = extractUserId(authHeader);
        LotResponse response = parkingLotService.createLot(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/{lotId} — PUBLIC
    // ─────────────────────────────────────────────────

    @GetMapping("/{lotId}")
    @Operation(summary = "Get parking lot by ID (public)")
    public ResponseEntity<LotResponse> getLotById(@PathVariable UUID lotId) {
        return ResponseEntity.ok(parkingLotService.getLotById(lotId));
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/city/{city} — PUBLIC
    // ─────────────────────────────────────────────────

    @GetMapping("/city/{city}")
    @Operation(summary = "Search lots by city (public)")
    public ResponseEntity<List<LotResponse>> getLotsByCity(@PathVariable String city) {
        return ResponseEntity.ok(parkingLotService.getLotsByCity(city));
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/nearby?lat=&lng=&radius= — PUBLIC
    // ─────────────────────────────────────────────────

    @GetMapping("/nearby")
    @Operation(summary = "GPS proximity search using Haversine formula (public)")
    public ResponseEntity<List<LotResponse>> getNearbyLots(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(parkingLotService.getNearbyLots(lat, lng, radius));
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/search?keyword= — PUBLIC
    // ─────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Keyword search on name, address, city (public)")
    public ResponseEntity<List<LotResponse>> searchLots(@RequestParam String keyword) {
        return ResponseEntity.ok(parkingLotService.searchLots(keyword));
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/manager/{managerId} — MANAGER (own) / ADMIN
    // ─────────────────────────────────────────────────

    @GetMapping("/manager/{managerId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get lots by manager ID (MANAGER or ADMIN)")
    public ResponseEntity<List<LotResponse>> getLotsByManager(
            @PathVariable UUID managerId,
            @RequestHeader("Authorization") String authHeader) {

        UUID callerId = extractUserId(authHeader);
        String role   = extractRole(authHeader);

        // Manager can only view their own lots; Admin can view any
        if (!"ADMIN".equals(role) && !callerId.equals(managerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(parkingLotService.getLotsByManager(managerId));
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/all — ADMIN only
    // ─────────────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all lots — approved + pending (ADMIN only)")
    public ResponseEntity<List<LotResponse>> getAllLots() {
        return ResponseEntity.ok(parkingLotService.getAllLots());
    }

    // ─────────────────────────────────────────────────
    // GET /api/v1/lots/pending — ADMIN only
    // ─────────────────────────────────────────────────

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get lots pending admin approval (ADMIN only)")
    public ResponseEntity<List<LotResponse>> getPendingLots() {
        return ResponseEntity.ok(parkingLotService.getPendingLots());
    }

    // ─────────────────────────────────────────────────
    // PUT /api/v1/lots/{lotId} — MANAGER (own) / ADMIN
    // ─────────────────────────────────────────────────

    @PutMapping("/{lotId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update lot details (MANAGER own lot or ADMIN)")
    public ResponseEntity<LotResponse> updateLot(
            @PathVariable UUID lotId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateLotRequest request) {

        UUID callerId = extractUserId(authHeader);
        String role   = extractRole(authHeader);

        // For ADMIN — pass any managerId (ownership not checked in service for admin)
        UUID requesterIdForService = "ADMIN".equals(role)
                ? parkingLotService.getLotById(lotId).getManagerId()  // admin acts as owner
                : callerId;

        return ResponseEntity.ok(parkingLotService.updateLot(lotId, requesterIdForService, request));
    }

    // ─────────────────────────────────────────────────
    // PUT /api/v1/lots/{lotId}/toggleOpen — MANAGER (own lot)
    // ─────────────────────────────────────────────────

    @PutMapping("/{lotId}/toggleOpen")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle lot open/closed status (MANAGER own lot only)")
    public ResponseEntity<LotResponse> toggleOpen(
            @PathVariable UUID lotId,
            @RequestHeader("Authorization") String authHeader) {

        UUID managerId = extractUserId(authHeader);
        return ResponseEntity.ok(parkingLotService.toggleOpen(lotId, managerId));
    }

    // ─────────────────────────────────────────────────
    // PUT /api/v1/lots/{lotId}/approve — ADMIN only
    // ─────────────────────────────────────────────────

    @PutMapping("/{lotId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Approve a pending lot registration (ADMIN only)")
    public ResponseEntity<LotResponse> approveLot(@PathVariable UUID lotId) {
        return ResponseEntity.ok(parkingLotService.approveLot(lotId));
    }

    // ─────────────────────────────────────────────────
    // PUT /api/v1/lots/{lotId}/decrement — Any valid JWT (booking-service)
    // ─────────────────────────────────────────────────

    @PutMapping("/{lotId}/decrement")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Decrement availableSpots — called by booking-service")
    public ResponseEntity<Void> decrementAvailable(@PathVariable UUID lotId) {
        parkingLotService.decrementAvailable(lotId);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────────
    // PUT /api/v1/lots/{lotId}/increment — Any valid JWT (booking-service)
    // ─────────────────────────────────────────────────

    @PutMapping("/{lotId}/increment")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Increment availableSpots — called by booking-service on checkout/cancel")
    public ResponseEntity<Void> incrementAvailable(@PathVariable UUID lotId) {
        parkingLotService.incrementAvailable(lotId);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────────
    // DELETE /api/v1/lots/{lotId} — MANAGER (own) / ADMIN
    // ─────────────────────────────────────────────────

    @DeleteMapping("/{lotId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a parking lot (MANAGER own lot or ADMIN)")
    public ResponseEntity<Void> deleteLot(
            @PathVariable UUID lotId,
            @RequestHeader("Authorization") String authHeader) {

        UUID callerId = extractUserId(authHeader);
        String role   = extractRole(authHeader);
        parkingLotService.deleteLot(lotId, callerId, role);
        return ResponseEntity.noContent().build();
    }
}