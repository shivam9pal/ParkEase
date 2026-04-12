package com.parkease.booking.repository;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // ─── Primary Lookup ───────────────────────────────────────────────────────
    Optional<Booking> findByBookingId(UUID bookingId);

    // ─── By Participant IDs ───────────────────────────────────────────────────
    List<Booking> findByUserId(UUID userId);

    List<Booking> findByLotId(UUID lotId);

    List<Booking> findBySpotId(UUID spotId);

    List<Booking> findByVehiclePlate(String vehiclePlate);

    // ─── By Status ────────────────────────────────────────────────────────────
    List<Booking> findByStatus(BookingStatus status);

    // Uniqueness check — a spot should only have ONE ACTIVE or RESERVED booking at a time
    Optional<Booking> findBySpotIdAndStatus(UUID spotId, BookingStatus status);

    // ─── Combined Filters ─────────────────────────────────────────────────────
    List<Booking> findByLotIdAndStatus(UUID lotId, BookingStatus status);

    // For analytics-service — revenue/occupancy reports over a date range
    List<Booking> findByLotIdAndStatusAndCreatedAtBetween(
            UUID lotId,
            BookingStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    // ─── Count Queries ────────────────────────────────────────────────────────
    // Used by manager dashboard — how many spots currently active in a lot
    long countByLotIdAndStatus(UUID lotId, BookingStatus status);

    // Used by analytics — count bookings in a specific status
    long countByStatus(BookingStatus status);

    // ─── Ordered Queries ─────────────────────────────────────────────────────
    // Driver's booking history — newest first
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // ─── Scheduler Query ─────────────────────────────────────────────────────
    /**
     * Finds all PRE_BOOKING bookings still in RESERVED status whose planned
     * startTime has passed the grace-period threshold.
     *
     * Called exclusively by BookingExpiryScheduler every 5 minutes.
     *
     * Example: if gracePeriodMinutes = 30, expiryThreshold = now - 30 min. Any
     * RESERVED booking whose startTime is before that threshold is expired.
     *
     * NOTE: Uses string literal 'RESERVED' and 'PRE_BOOKING' — these must match
     * BookingStatus and BookingType enum names exactly.
     */
    @Query("SELECT b FROM Booking b "
            + "WHERE b.status = 'RESERVED' "
            + "AND b.bookingType = 'PRE_BOOKING' "
            + "AND b.startTime < :expiryThreshold")
    List<Booking> findExpiredPreBookings(@Param("expiryThreshold") LocalDateTime expiryThreshold);
}
