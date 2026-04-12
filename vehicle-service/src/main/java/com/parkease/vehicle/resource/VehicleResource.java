package com.parkease.vehicle.resource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.vehicle.dto.RegisterVehicleRequest;
import com.parkease.vehicle.dto.UpdateVehicleRequest;
import com.parkease.vehicle.dto.VehicleResponse;
import com.parkease.vehicle.entity.VehicleType;
import com.parkease.vehicle.service.VehicleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for vehicle-service.
 *
 * Base path : /api/v1/vehicles Port : 8086
 *
 * ── Auth Rules ── DRIVER → can only manage their own vehicles (ownerId == JWT
 * userId) ADMIN → can access any vehicle / all vehicles Any JWT → can read
 * vehicle type and EV status (used by booking-service)
 *
 * ── ownerId source ── ALWAYS extracted from the validated JWT token (via
 * SecurityContext). NEVER accepted from request body — prevents privilege
 * escalation.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vehicle API", description = "Vehicle registration and management for ParkEase drivers")
public class VehicleResource {

    private final VehicleService vehicleService;

    // ═══════════════════════════════════════════════════════════════════════
    // 1. POST /api/v1/vehicles/register
    //    Register a new vehicle — DRIVER only
    //    ownerId is extracted from JWT, never from request body
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Register a new vehicle",
            description = "Registers a vehicle for the authenticated driver. "
            + "The ownerId is automatically extracted from the JWT token — "
            + "it must NOT be passed in the request body."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vehicle registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate license plate"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PostMapping("/register")
    public ResponseEntity<VehicleResponse> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequest request
    ) {
        UUID ownerId = getLoggedInUserId();
        log.info("POST /register — ownerId={}, plate={}", ownerId, request.getLicensePlate());

        VehicleResponse response = vehicleService.registerVehicle(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. GET /api/v1/vehicles/{vehicleId}
    //    Get vehicle by ID — DRIVER (own) or ADMIN
    //    Also called by booking-service via RestTemplate
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Get vehicle by ID",
            description = "Fetches a vehicle by its UUID. "
            + "A DRIVER can only retrieve their own vehicle. "
            + "ADMIN can retrieve any vehicle. "
            + "booking-service also calls this endpoint via RestTemplate."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle found"),
        @ApiResponse(responseCode = "403", description = "Driver accessing another driver's vehicle"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @Parameter(description = "Vehicle UUID") @PathVariable UUID vehicleId
    ) {
        long startTime = System.currentTimeMillis();
        log.debug("[VehicleResource] GET /{} — requester={}", vehicleId, getLoggedInUserId());

        VehicleResponse response = vehicleService.getVehicleById(vehicleId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[VehicleResource] GET /{} completed in {}ms", vehicleId, duration);

        // ── Owner check: DRIVER can only view their own vehicle ──
        enforceOwnerOrAdmin(response.getOwnerId());

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. GET /api/v1/vehicles/owner/{ownerId}
    //    Get all vehicles for a driver — DRIVER (own) or ADMIN
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Get all vehicles by owner",
            description = "Returns all vehicles registered to a specific driver. "
            + "A DRIVER can only query their own ownerId. "
            + "ADMIN can query any ownerId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle list returned (empty list if none)"),
        @ApiResponse(responseCode = "403", description = "Driver querying another driver's vehicles")
    })
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByOwner(
            @Parameter(description = "Owner UUID (driver's userId from auth-service)")
            @PathVariable UUID ownerId
    ) {
        log.debug("GET /owner/{} — requester={}", ownerId, getLoggedInUserId());

        // ── Owner check before DB call ──
        enforceOwnerOrAdmin(ownerId);

        List<VehicleResponse> vehicles = vehicleService.getVehiclesByOwner(ownerId);
        return ResponseEntity.ok(vehicles);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. GET /api/v1/vehicles/plate/{licensePlate}
    //    Find vehicle by license plate — DRIVER or ADMIN
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Find vehicle by license plate",
            description = "Global license plate lookup. "
            + "A DRIVER can only retrieve their own vehicle's data. "
            + "ADMIN can retrieve any vehicle."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle found"),
        @ApiResponse(responseCode = "403", description = "Driver accessing another driver's vehicle"),
        @ApiResponse(responseCode = "404", description = "No vehicle with that plate")
    })
    @GetMapping("/plate/{licensePlate}")
    public ResponseEntity<VehicleResponse> getByLicensePlate(
            @Parameter(description = "License plate string (case-insensitive)")
            @PathVariable String licensePlate
    ) {
        log.debug("GET /plate/{} — requester={}", licensePlate, getLoggedInUserId());

        VehicleResponse response = vehicleService.getByLicensePlate(licensePlate);

        // ── Owner check ──
        enforceOwnerOrAdmin(response.getOwnerId());

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. GET /api/v1/vehicles/all
    //    Get all vehicles — ADMIN only
    //    SecurityConfig enforces hasRole("ADMIN") at the filter level
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Get all vehicles [ADMIN]",
            description = "Returns every vehicle in the system regardless of owner. "
            + "Requires ADMIN role — enforced at both SecurityConfig and method level."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Full vehicle list returned"),
        @ApiResponse(responseCode = "403", description = "Non-ADMIN attempting access")
    })
    @GetMapping("/all")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        log.debug("GET /all — admin request from userId={}", getLoggedInUserId());

        List<VehicleResponse> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. PUT /api/v1/vehicles/{vehicleId}
    //    Update vehicle — DRIVER (own vehicle only)
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Update vehicle details",
            description = "Partially updates a vehicle's make, model, color, vehicleType, or isEV flag. "
            + "Only the vehicle owner (DRIVER) can update. "
            + "licensePlate and ownerId cannot be changed after registration."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle updated successfully"),
        @ApiResponse(responseCode = "403", description = "Driver updating another driver's vehicle"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @Parameter(description = "Vehicle UUID") @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        log.info("PUT /{} — requester={}", vehicleId, getLoggedInUserId());

        // ── Must fetch first to verify ownership before allowing update ──
        VehicleResponse existing = vehicleService.getVehicleById(vehicleId);
        enforceOwnerOnly(existing.getOwnerId());

        VehicleResponse updated = vehicleService.updateVehicle(vehicleId, request);
        return ResponseEntity.ok(updated);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. DELETE /api/v1/vehicles/{vehicleId}
    //    Soft delete — DRIVER (own vehicle only)
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Delete (soft-delete) a vehicle",
            description = "Sets the vehicle's isActive flag to false. "
            + "Records are never hard-deleted — booking history depends on them. "
            + "Only the vehicle owner (DRIVER) can delete."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vehicle deleted (soft)"),
        @ApiResponse(responseCode = "403", description = "Driver deleting another driver's vehicle"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Vehicle UUID") @PathVariable UUID vehicleId
    ) {
        log.info("DELETE /{} — requester={}", vehicleId, getLoggedInUserId());

        VehicleResponse existing = vehicleService.getVehicleById(vehicleId);
        enforceOwnerOnly(existing.getOwnerId());

        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();   // 204 No Content
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 8. GET /api/v1/vehicles/{vehicleId}/type
    //    Get vehicle type — any authenticated user
    //    Called by booking-service to validate spot compatibility
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Get vehicle type",
            description = "Returns the VehicleType enum string (TWO_WHEELER / FOUR_WHEELER / HEAVY). "
            + "Used by booking-service to validate spot compatibility at booking time."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "VehicleType returned as plain string"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/{vehicleId}/type")
    public ResponseEntity<VehicleType> getVehicleType(
            @Parameter(description = "Vehicle UUID") @PathVariable UUID vehicleId
    ) {
        log.debug("GET /{}/type", vehicleId);

        VehicleType type = vehicleService.getVehicleType(vehicleId);
        return ResponseEntity.ok(type);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 9. GET /api/v1/vehicles/{vehicleId}/isEV
    //    Check EV status — any authenticated user
    //    booking-service calls this to assign EV-charging spots
    // ═══════════════════════════════════════════════════════════════════════
    @Operation(
            summary = "Check if vehicle is electric (EV)",
            description = "Returns true/false. "
            + "booking-service calls this via RestTemplate to determine whether "
            + "to assign the vehicle to an EV-charging spot."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boolean EV status returned"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/{vehicleId}/isEV")
    public ResponseEntity<Boolean> isEVVehicle(
            @Parameter(description = "Vehicle UUID") @PathVariable UUID vehicleId
    ) {
        log.debug("GET /{}/isEV", vehicleId);

        boolean isEV = vehicleService.isEVVehicle(vehicleId);
        return ResponseEntity.ok(isEV);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS — SecurityContext extraction + owner checks
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Extracts the logged-in user's UUID from Spring SecurityContext.
     * JwtAuthFilter stores it in authentication.getDetails() as a Map.
     */
    @SuppressWarnings("unchecked")
    private UUID getLoggedInUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var details = (Map<String, Object>) auth.getDetails();
        return (UUID) details.get("userId");
    }

    /**
     * Extracts the logged-in user's role string (e.g. "DRIVER", "ADMIN").
     */
    @SuppressWarnings("unchecked")
    private String getLoggedInUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var details = (Map<String, Object>) auth.getDetails();
        return (String) details.get("role");
    }

    /**
     * DRIVER can only access their own resource. ADMIN can access any resource.
     *
     * Throws AccessDeniedException (→ 403) if a DRIVER tries to access a
     * vehicle belonging to a different owner.
     */
    private void enforceOwnerOrAdmin(UUID resourceOwnerId) {
        String role = getLoggedInUserRole();
        UUID loggedInUserId = getLoggedInUserId();

        if ("ADMIN".equals(role)) {
            return; // Admin passes all checks
        }
        if (!loggedInUserId.equals(resourceOwnerId)) {
            log.warn("403: userId={} attempted to access resource owned by {}",
                    loggedInUserId, resourceOwnerId);
            throw new AccessDeniedException(
                    "You can only access your own vehicles"
            );
        }
    }

    /**
     * Strict owner-only check — ADMIN cannot update or delete another user's
     * vehicle (by design: only the vehicle owner can mutate their vehicle).
     *
     * Throws AccessDeniedException (→ 403) for any mismatch.
     */
    private void enforceOwnerOnly(UUID resourceOwnerId) {
        UUID loggedInUserId = getLoggedInUserId();

        if (!loggedInUserId.equals(resourceOwnerId)) {
            log.warn("403: userId={} attempted to mutate resource owned by {}",
                    loggedInUserId, resourceOwnerId);
            throw new AccessDeniedException(
                    "You can only modify your own vehicles"
            );
        }
    }
}
