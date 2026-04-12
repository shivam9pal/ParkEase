package com.parkease.booking.resource;

import com.parkease.booking.dto.*;
import com.parkease.booking.security.JwtAuthFilter;
import com.parkease.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for booking-service. All endpoints under /api/v1/bookings.
 *
 * userId and role are ALWAYS extracted from the JWT via SecurityContext — never
 * accepted from request body or query parameter.
 *
 * Authorization summary: SecurityConfig → route-level role guards (DRIVER /
 * MANAGER / ADMIN) BookingService → ownership checks and business-level access
 * control
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking", description = "Booking lifecycle management — create, check-in, check-out, cancel, extend")
@SecurityRequirement(name = "bearerAuth")
public class BookingResource {

    private final BookingService bookingService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/bookings — Create Booking (DRIVER only)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(
            summary = "Create a new booking",
            description = "Creates a PRE_BOOKING (RESERVED) or WALK_IN (ACTIVE) booking. "
            + "Validates vehicle ownership, spot availability, and type compatibility."
    )
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("[BookingResource] POST /bookings — userId={}, spotId={}, type={}",
                userId, request.getSpotId(), request.getBookingType());

        BookingResponse response = bookingService.createBooking(userId, request, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/all — All Bookings Platform-Wide (ADMIN only)
    // IMPORTANT: Must be declared BEFORE /api/v1/bookings/{bookingId}
    // to prevent Spring mapping "all" as a UUID path variable
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    @Operation(
            summary = "Get all bookings [ADMIN]",
            description = "Returns every booking across all lots and users. Admin access only."
    )
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        log.info("[BookingResource] GET /bookings/all");
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/my — Driver's Own Bookings
    // IMPORTANT: Must be declared BEFORE /{bookingId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/my")
    @Operation(
            summary = "Get my bookings",
            description = "Returns all bookings (all statuses) for the authenticated driver."
    )
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("[BookingResource] GET /bookings/my — userId={}", userId);
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/history — Driver's Booking History
    // IMPORTANT: Must be declared BEFORE /{bookingId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/history")
    @Operation(
            summary = "Get booking history",
            description = "Returns COMPLETED and CANCELLED bookings for the authenticated driver, newest first."
    )
    public ResponseEntity<List<BookingResponse>> getBookingHistory(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("[BookingResource] GET /bookings/history — userId={}", userId);
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/lot/{lotId} — All Bookings for a Lot (MANAGER/ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/lot/{lotId}")
    @Operation(
            summary = "Get all bookings for a lot [MANAGER/ADMIN]",
            description = "Returns all bookings (any status) for the specified lot."
    )
    public ResponseEntity<List<BookingResponse>> getBookingsByLot(
            @PathVariable UUID lotId) {
        log.info("[BookingResource] GET /bookings/lot/{}", lotId);
        return ResponseEntity.ok(bookingService.getBookingsByLot(lotId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/lot/{lotId}/active — Active Bookings for a Lot
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/lot/{lotId}/active")
    @Operation(
            summary = "Get active bookings for a lot [MANAGER/ADMIN]",
            description = "Returns only ACTIVE bookings for the specified lot — real-time occupancy view."
    )
    public ResponseEntity<List<BookingResponse>> getActiveBookings(
            @PathVariable UUID lotId) {
        log.info("[BookingResource] GET /bookings/lot/{}/active", lotId);
        return ResponseEntity.ok(bookingService.getActiveBookings(lotId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/{bookingId} — Get Booking by ID
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Get booking by ID",
            description = "Returns a single booking. DRIVER must own the booking; MANAGER/ADMIN can access any."
    )
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        BookingResponse booking = bookingService.getBookingById(bookingId);

        // Ownership enforcement — DRIVER can only view own booking
        if ("DRIVER".equalsIgnoreCase(role) && !booking.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(booking);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/{bookingId}/fare — Fare Estimate
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{bookingId}/fare")
    @Operation(
            summary = "Get fare estimate",
            description = "Returns a real-time fare estimate for an ACTIVE booking. Read-only — does not persist."
    )
    public ResponseEntity<FareCalculationResponse> getFareEstimate(
            @PathVariable UUID bookingId) {
        log.info("[BookingResource] GET /bookings/{}/fare", bookingId);
        return ResponseEntity.ok(bookingService.calculateAmount(bookingId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/bookings/{bookingId}/checkin — Check In (DRIVER only)
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{bookingId}/checkin")
    @Operation(
            summary = "Check in to a PRE_BOOKING",
            description = "Transitions a RESERVED PRE_BOOKING to ACTIVE. Sets checkInTime. "
            + "Occupies the spot in spot-service."
    )
    public ResponseEntity<BookingResponse> checkIn(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("[BookingResource] PUT /bookings/{}/checkin — userId={}", bookingId, userId);
        return ResponseEntity.ok(bookingService.checkIn(bookingId, userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/bookings/{bookingId}/checkout — Check Out (DRIVER/MANAGER/ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{bookingId}/checkout")
    @Operation(
            summary = "Check out of a booking",
            description = "Transitions ACTIVE → COMPLETED. Calculates fare (min 1 hour). "
            + "Releases spot and increments lot counter. "
            + "Publishes booking.checkout event for payment-service."
    )
    public ResponseEntity<BookingResponse> checkOut(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        log.info("[BookingResource] PUT /bookings/{}/checkout — userId={}, role={}",
                bookingId, userId, role);

        // MANAGER/ADMIN can check out any booking — pass their userId as a
        // bypass signal. Service enforces ownership only for DRIVER.
        // For MANAGER/ADMIN, we pass the booking's actual userId by fetching it.
        UUID effectiveUserId = userId;
        if ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            // Fetch booking to get the owner's userId — allows service ownership check to pass
            BookingResponse existing = bookingService.getBookingById(bookingId);
            effectiveUserId = existing.getUserId();
        }

        return ResponseEntity.ok(bookingService.checkOut(bookingId, effectiveUserId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/bookings/{bookingId}/cancel — Cancel Booking
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancels a RESERVED or ACTIVE booking. "
            + "DRIVER: own bookings only. MANAGER: lot-scoped. ADMIN: any booking."
    )
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        String role = extractRole(authentication);

        log.info("[BookingResource] PUT /bookings/{}/cancel — userId={}, role={}",
                bookingId, userId, role);

        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, userId, role));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/bookings/{bookingId}/extend — Extend Booking (DRIVER only)
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{bookingId}/extend")
    @Operation(
            summary = "Extend booking end time",
            description = "Extends the planned endTime of a RESERVED or ACTIVE booking. "
            + "No spot changes — spot is already held. Fare recalculated at actual checkOut."
    )
    public ResponseEntity<BookingResponse> extendBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ExtendBookingRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        log.info("[BookingResource] PUT /bookings/{}/extend — userId={}, newEndTime={}",
                bookingId, userId, request.getNewEndTime());

        return ResponseEntity.ok(bookingService.extendBooking(bookingId, userId, request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/bookings/stats — Booking Statistics (ADMIN/SYSTEM only)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @Operation(
            summary = "Get booking statistics for a date range [ADMIN/SYSTEM]",
            description = "Returns counts of active, completed, and cancelled bookings for the specified date range. "
            + "Used by analytics-service for platform summary. No JWT required (system service call via Feign)."
    )
    public ResponseEntity<BookingStatsResponse> getBookingStats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("[BookingResource] GET /bookings/stats — from={}, to={}", from, to);
        return ResponseEntity.ok(bookingService.getBookingStats(from, to));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — JWT Claims Extraction from SecurityContext
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Extracts userId UUID from the JWT details attached by JwtAuthFilter.
     * Never re-parses the token — reads from already-populated SecurityContext.
     */
    private UUID extractUserId(Authentication authentication) {
        JwtAuthFilter.WebAuthenticationDetailsSourceWrapper details
                = (JwtAuthFilter.WebAuthenticationDetailsSourceWrapper) authentication.getDetails();
        return details.getUserId();
    }

    /**
     * Extracts role string from the JWT details attached by JwtAuthFilter.
     * Returns "DRIVER", "MANAGER", or "ADMIN" — uppercase.
     */
    private String extractRole(Authentication authentication) {
        JwtAuthFilter.WebAuthenticationDetailsSourceWrapper details
                = (JwtAuthFilter.WebAuthenticationDetailsSourceWrapper) authentication.getDetails();
        return details.getRole();
    }
}
