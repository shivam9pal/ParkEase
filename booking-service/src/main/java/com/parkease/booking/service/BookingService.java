package com.parkease.booking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.parkease.booking.dto.BookingResponse;
import com.parkease.booking.dto.BookingStatsResponse;
import com.parkease.booking.dto.CreateBookingRequest;
import com.parkease.booking.dto.ExtendBookingRequest;
import com.parkease.booking.dto.FareCalculationResponse;

public interface BookingService {

    // ─── Core Lifecycle ───────────────────────────────────────────────────────
    /**
     * Creates a new booking (PRE_BOOKING or WALK_IN).
     *
     * PRE_BOOKING → status RESERVED, spot RESERVED, checkInTime null WALK_IN →
     * status ACTIVE, spot OCCUPIED, checkInTime = now
     *
     * Orchestrates: vehicle validation → spot validation → reserve/occupy spot
     * → decrement lot counter → save → publish event.
     *
     * @param userId Extracted from JWT in controller — never from request body
     * @param request Spot, vehicle, type, and time window
     * @param jwtToken Full "Bearer ..." header — forwarded to Feign clients
     */
    BookingResponse createBooking(UUID userId, CreateBookingRequest request, String jwtToken);

    /**
     * Transitions a PRE_BOOKING from RESERVED → ACTIVE. Calls spot-service
     * occupySpot(). Sets checkInTime = now.
     *
     * @throws IllegalStateException if status != RESERVED
     * @throws SecurityException if booking.userId != JWT userId
     */
    BookingResponse checkIn(UUID bookingId, UUID userId);

    /**
     * Transitions ACTIVE → COMPLETED. Calculates fare based on actual
     * checkInTime → now (min 1 hour). Releases spot, increments lot counter,
     * persists totalAmount.
     *
     * @throws IllegalStateException if status != ACTIVE
     */
    BookingResponse checkOut(UUID bookingId, UUID userId);

    /**
     * Cancels a booking in RESERVED or ACTIVE status. Releases spot, increments
     * lot counter.
     *
     * @param requesterRole "DRIVER", "MANAGER", or "ADMIN" — controls access
     * scope
     * @throws IllegalStateException if status is COMPLETED or already CANCELLED
     * @throws SecurityException if DRIVER tries to cancel another user's
     * booking
     */
    BookingResponse cancelBooking(UUID bookingId, UUID userId, String requesterRole);

    /**
     * Extends the planned endTime of a RESERVED or ACTIVE booking. No spot
     * changes — spot is already held. Fare still calculated at checkOut.
     *
     * @throws IllegalArgumentException if newEndTime <= current endTime
     */
    BookingResponse extendBooking(UUID bookingId, UUID userId, ExtendBookingRequest request);

    // ─── Queries ─────────────────────────────────────────────────────────────
    /**
     * Fetch a single booking by ID. Caller's authorization (owner or
     * MANAGER/ADMIN) enforced in controller.
     *
     * @throws RuntimeException (404) if not found
     */
    BookingResponse getBookingById(UUID bookingId);

    /**
     * All bookings made by a specific driver — ordered by createdAt DESC.
     * Returns empty list (never 404) if driver has no bookings.
     */
    List<BookingResponse> getBookingsByUser(UUID userId);

    /**
     * All bookings (any status) for a given parking lot. Used by MANAGER/ADMIN
     * dashboard.
     */
    List<BookingResponse> getBookingsByLot(UUID lotId);

    /**
     * Only ACTIVE bookings for a given lot — real-time occupancy view.
     */
    List<BookingResponse> getActiveBookings(UUID lotId);

    /**
     * COMPLETED + CANCELLED bookings for a driver — their historical record.
     * Ordered newest-first via repository.
     */
    List<BookingResponse> getBookingHistory(UUID userId);

    /**
     * All bookings platform-wide — ADMIN only. Controller enforces ADMIN role
     * before calling this.
     */
    List<BookingResponse> getAllBookings();

    // ─── Fare Calculation ────────────────────────────────────────────────────
    /**
     * Real-time fare estimate for an ACTIVE booking. Uses booking.pricePerHour
     * (snapshotted at creation) × elapsed hours. Minimum 1 hour enforced.
     *
     * Does NOT persist anything — read-only estimate.
     *
     * @throws IllegalStateException if booking is not ACTIVE (must be checked
     * in)
     */
    FareCalculationResponse calculateAmount(UUID bookingId);

    // ─── System / Scheduler ──────────────────────────────────────────────────
    /**
     * Auto-cancels expired PRE_BOOKING reservations. Called by
     * BookingExpiryScheduler every 5 minutes.
     *
     * Finds all RESERVED PRE_BOOKINGs where startTime < (now -
     * gracePeriodMinutes). For each: releases spot → increments lot → sets
     * CANCELLED → publishes event.
     *
     * This method is intentionally package-accessible to the scheduler only. No
     * JWT context — runs as system operation.
     */
    void autoExpireBookings();

    // ─── Analytics ───────────────────────────────────────────────────────────
    /**
     * Gets booking statistics for a date range. Used by analytics-service to
     * populate platform summary.
     *
     * @param from Start of the date range (inclusive)
     * @param to End of the date range (inclusive)
     * @return BookingStatsResponse with active, completed, and cancelled counts
     */
    BookingStatsResponse getBookingStats(LocalDateTime from, LocalDateTime to);
}
