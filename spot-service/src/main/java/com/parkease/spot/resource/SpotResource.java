package com.parkease.spot.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.spot.dto.AddSpotRequest;
import com.parkease.spot.dto.BulkAddSpotRequest;
import com.parkease.spot.dto.SpotResponse;
import com.parkease.spot.dto.UpdateSpotRequest;
import com.parkease.spot.entity.SpotType;
import com.parkease.spot.entity.VehicleType;
import com.parkease.spot.service.SpotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for spot-service. All endpoints are versioned under
 * /api/v1/spots.
 *
 * Public endpoints → no JWT required (guests browse spots) Protected endpoints
 * → JWT required (managers manage, booking-service transitions)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
@Tag(name = "Parking Spot API",
        description = "Manage individual parking spots within a lot — create, browse, and status transitions")
public class SpotResource {

    private final SpotService spotService;

    // ══════════════════════════════════════════════════════════════════════════
    //  CREATE — MANAGER only
    // ══════════════════════════════════════════════════════════════════════════
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add a single spot to a lot",
            description = "Creates one parking spot in the specified lot. "
            + "spotNumber must be unique within the lot. "
            + "If spotType = EV, isEVCharging is automatically set to true."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Spot created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate spot number"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "User does not have MANAGER role")
    })
    public ResponseEntity<SpotResponse> addSpot(
            @Parameter(description = "UUID of the parking lot", required = true)
            @RequestParam UUID lotId,
            @Valid @RequestBody AddSpotRequest request) {

        log.info("POST /api/v1/spots — lotId={}, spotNumber={}",
                lotId, request.getSpotNumber());
        SpotResponse response = spotService.addSpot(lotId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Bulk add spots to a lot",
            description = "Creates multiple spots in one request using auto-generated sequential numbers. "
            + "Example: prefix='A', count=5 → A-01, A-02, A-03, A-04, A-05. "
            + "If prefix is omitted, spotType name is used (e.g. COMPACT-01)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Spots created — returns list of all created spots"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "User does not have MANAGER role")
    })
    public ResponseEntity<List<SpotResponse>> addBulkSpots(
            @Parameter(description = "UUID of the parking lot", required = true)
            @RequestParam UUID lotId,
            @Valid @RequestBody BulkAddSpotRequest request) {

        log.info("POST /api/v1/spots/bulk — lotId={}, count={}",
                lotId, request.getCount());
        List<SpotResponse> responses = spotService.addBulkSpots(lotId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  READ — PUBLIC (no JWT required)
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/{spotId}")
    @Operation(
            summary = "Get spot by ID",
            description = "Returns full spot details including pricePerHour. "
            + "Used by booking-service for fare calculation. Public — no JWT required."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot found"),
        @ApiResponse(responseCode = "404", description = "Spot not found")
    })
    public ResponseEntity<SpotResponse> getSpotById(
            @Parameter(description = "UUID of the spot", required = true)
            @PathVariable UUID spotId) {

        return ResponseEntity.ok(spotService.getSpotById(spotId));
    }

    @GetMapping("/lot/{lotId}")
    @Operation(
            summary = "Get all spots in a lot",
            description = "Returns every spot in the lot regardless of status. "
            + "Used by analytics-service. Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getSpotsByLot(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(spotService.getSpotsByLot(lotId));
    }

    @GetMapping("/lot/{lotId}/available")
    @Operation(
            summary = "Get only AVAILABLE spots in a lot",
            description = "Primary listing for drivers browsing before booking. Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getAvailableSpots(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(spotService.getAvailableSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/type/{spotType}")
    @Operation(
            summary = "Get spots filtered by spot type",
            description = "Returns all spots of the given type (COMPACT/STANDARD/LARGE/MOTORBIKE/EV). "
            + "Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getBySpotType(
            @PathVariable UUID lotId,
            @PathVariable SpotType spotType) {

        return ResponseEntity.ok(spotService.getByTypeAndLot(lotId, spotType));
    }

    @GetMapping("/lot/{lotId}/vehicle/{vehicleType}")
    @Operation(
            summary = "Get spots filtered by vehicle type compatibility",
            description = "Returns spots matching the vehicle type. "
            + "Used by booking-service to find spots compatible with the driver's vehicle. "
            + "Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getByVehicleType(
            @PathVariable UUID lotId,
            @PathVariable VehicleType vehicleType) {

        return ResponseEntity.ok(spotService.getByVehicleTypeAndLot(lotId, vehicleType));
    }

    @GetMapping("/lot/{lotId}/floor/{floor}")
    @Operation(
            summary = "Get spots on a specific floor",
            description = "Supports floor-plan frontend view. "
            + "Floor 0 = Ground, 1 = First Floor, -1 = Basement 1. "
            + "Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getByFloor(
            @PathVariable UUID lotId,
            @PathVariable Integer floor) {

        return ResponseEntity.ok(spotService.getByFloorAndLot(lotId, floor));
    }

    @GetMapping("/lot/{lotId}/ev")
    @Operation(
            summary = "Get EV charging spots in a lot",
            description = "Returns all spots with isEVCharging=true. Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getEVSpots(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(spotService.getEVSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/handicapped")
    @Operation(
            summary = "Get handicapped accessible spots in a lot",
            description = "Returns all spots with isHandicapped=true. Public — no JWT required."
    )
    public ResponseEntity<List<SpotResponse>> getHandicappedSpots(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(spotService.getHandicappedSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/count")
    @Operation(
            summary = "Count AVAILABLE spots in a lot",
            description = "Returns a single Long value — count of AVAILABLE spots. "
            + "Used by analytics-service. Public — no JWT required."
    )
    public ResponseEntity<Long> countAvailableSpots(
            @PathVariable UUID lotId) {

        return ResponseEntity.ok(spotService.countAvailable(lotId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATUS TRANSITIONS — Any valid JWT (booking-service internal calls)
    // ══════════════════════════════════════════════════════════════════════════
    @PutMapping("/{spotId}/reserve")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Reserve a spot — AVAILABLE → RESERVED",
            description = "Called by booking-service on booking creation. "
            + "Requires any valid JWT. Returns 409 if spot is not AVAILABLE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot reserved"),
        @ApiResponse(responseCode = "404", description = "Spot not found"),
        @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<SpotResponse> reserveSpot(
            @PathVariable UUID spotId) {

        log.info("PUT /api/v1/spots/{}/reserve", spotId);
        return ResponseEntity.ok(spotService.reserveSpot(spotId));
    }

    @PutMapping("/{spotId}/occupy")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Occupy a spot — RESERVED/AVAILABLE → OCCUPIED",
            description = "Called by booking-service on driver check-in. "
            + "Accepts RESERVED (normal check-in) or AVAILABLE (walk-in). "
            + "Returns 409 if spot is already OCCUPIED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot occupied"),
        @ApiResponse(responseCode = "404", description = "Spot not found"),
        @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<SpotResponse> occupySpot(
            @PathVariable UUID spotId) {

        log.info("PUT /api/v1/spots/{}/occupy", spotId);
        return ResponseEntity.ok(spotService.occupySpot(spotId));
    }

    @PutMapping("/{spotId}/release")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Release a spot — RESERVED/OCCUPIED → AVAILABLE",
            description = "Called by booking-service on cancellation or checkout. "
            + "Returns 409 if spot is already AVAILABLE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot released back to AVAILABLE"),
        @ApiResponse(responseCode = "404", description = "Spot not found"),
        @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<SpotResponse> releaseSpot(
            @PathVariable UUID spotId) {

        log.info("PUT /api/v1/spots/{}/release", spotId);
        return ResponseEntity.ok(spotService.releaseSpot(spotId));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/spots/{spotId}/maintenance — Toggle maintenance status
    // ──────────────────────────────────────────────────────────────────────────
    @PutMapping("/{spotId}/maintenance")
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Toggle spot maintenance status",
            description = "AVAILABLE ↔ MAINTENANCE toggle. "
            + "Transitions: AVAILABLE → MAINTENANCE (starts maintenance), "
            + "MAINTENANCE → AVAILABLE (ends maintenance). "
            + "Cannot put RESERVED or OCCUPIED spots under maintenance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status toggled successfully"),
        @ApiResponse(responseCode = "400", description = "Booking conflict / Invalid JWT"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "User does not have MANAGER role"),
        @ApiResponse(responseCode = "404", description = "Spot not found"),
        @ApiResponse(responseCode = "409", description = "Spot is RESERVED or OCCUPIED — cannot set to maintenance")
    })
    public ResponseEntity<SpotResponse> toggleMaintenance(
            @PathVariable UUID spotId) {

        log.info("PUT /api/v1/spots/{}/maintenance", spotId);
        return ResponseEntity.ok(spotService.toggleMaintenance(spotId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UPDATE — MANAGER only
    // ══════════════════════════════════════════════════════════════════════════
    @PutMapping("/{spotId}")
    @PreAuthorize("hasRole('MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update spot metadata",
            description = "Partial update — only non-null fields are applied. "
            + "spotNumber and lotId are immutable and cannot be changed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spot updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "User does not have MANAGER role"),
        @ApiResponse(responseCode = "404", description = "Spot not found")
    })
    public ResponseEntity<SpotResponse> updateSpot(
            @PathVariable UUID spotId,
            @Valid @RequestBody UpdateSpotRequest request) {

        log.info("PUT /api/v1/spots/{}", spotId);
        return ResponseEntity.ok(spotService.updateSpot(spotId, request));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE — MANAGER or ADMIN
    // ══════════════════════════════════════════════════════════════════════════
    @DeleteMapping("/{spotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete a spot",
            description = "Permanently removes the spot record. MANAGER or ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Spot deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Spot not found")
    })
    public ResponseEntity<Void> deleteSpot(
            @PathVariable UUID spotId) {

        log.info("DELETE /api/v1/spots/{}", spotId);
        spotService.deleteSpot(spotId);
        return ResponseEntity.noContent().build();
    }
}
