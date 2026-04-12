package com.parkease.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkease.booking.dto.BookingResponse;
import com.parkease.booking.dto.BookingStatsResponse;
import com.parkease.booking.dto.CreateBookingRequest;
import com.parkease.booking.dto.ExtendBookingRequest;
import com.parkease.booking.dto.FareCalculationResponse;
import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.BookingStatus;
import com.parkease.booking.entity.BookingType;
import com.parkease.booking.entity.VehicleType;
import com.parkease.booking.feign.ParkingLotServiceClient;
import com.parkease.booking.feign.SpotServiceClient;
import com.parkease.booking.feign.VehicleServiceClient;
import com.parkease.booking.feign.dto.SpotResponse;
import com.parkease.booking.feign.dto.VehicleResponse;
import com.parkease.booking.messaging.BookingEventPublisher;
import com.parkease.booking.repository.BookingRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final SpotServiceClient spotServiceClient;
    private final ParkingLotServiceClient parkingLotServiceClient;
    private final VehicleServiceClient vehicleServiceClient;
    private final BookingEventPublisher bookingEventPublisher;

    @Value("${booking.expiry.grace-period-minutes:30}")
    private int gracePeriodMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CREATE BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Full orchestration flow: Vehicle validation → Spot validation →
     * Reserve/Occupy spot → Decrement lot counter → Save DB → Publish event
     *
     * ROLLBACK RULES (Feign calls are NOT rolled back by @Transactional): Step
     * 5 fails (reserve/occupy) → nothing to undo Step 6 fails (decrement lot) →
     * releaseSpot() Step 7 fails (DB save) → releaseSpot() +
     * incrementAvailableSpots()
     */
    @Override
    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request, String jwtToken) {

        // ── Step 1: Validate time window ──────────────────────────────────────
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("startTime cannot be in the past.");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }

        // ── Step 2: Fetch and validate vehicle ────────────────────────────────
        VehicleResponse vehicle;
        try {
            log.debug("[BookingService] Fetching vehicle from vehicle-service: vehicleId={}", request.getVehicleId());
            vehicle = vehicleServiceClient.getVehicleById(request.getVehicleId());
        } catch (FeignException.NotFound e) {
            log.warn("[BookingService] Vehicle not found: vehicleId={}, status={}", request.getVehicleId(), e.status());
            throw new RuntimeException("Vehicle not found with id: " + request.getVehicleId());
        } catch (FeignException.ServiceUnavailable e) {
            log.error("[BookingService] Vehicle service returned 503: {}", e.getMessage());
            throw new RuntimeException("Vehicle service unavailable. Please try again later.");
        } catch (FeignException.Unauthorized e) {
            log.error("[BookingService] Unauthorized access to vehicle-service: {}", e.getMessage());
            throw new RuntimeException("Authorization failed when accessing vehicle service.");
        } catch (FeignException.BadRequest e) {
            log.error("[BookingService] Bad request to vehicle-service: {}", e.getMessage());
            throw new RuntimeException("Invalid vehicle request: " + e.contentUTF8());
        } catch (FeignException e) {
            log.error("[BookingService] FeignException from vehicle-service - Status: {}, Message: {}, Body: {}",
                    e.status(), e.getMessage(), e.contentUTF8(), e);
            throw new RuntimeException("Vehicle service unavailable. Please try again later.");
        }

        // Ownership check — driver cannot book with someone else's vehicle
        if (!vehicle.getOwnerId().equals(userId)) {
            throw new SecurityException("Vehicle does not belong to the requesting user.");
        }

        // Soft-delete check — inactive vehicles cannot be booked
        if (Boolean.FALSE.equals(vehicle.getIsActive())) {
            throw new IllegalArgumentException("Vehicle is deactivated and cannot be used for booking.");
        }

        // ── Step 3: Fetch and validate spot ───────────────────────────────────
        SpotResponse spot;
        try {
            log.debug("[BookingService] Fetching spot from spot-service: spotId={}", request.getSpotId());
            spot = spotServiceClient.getSpotById(request.getSpotId());
        } catch (FeignException.NotFound e) {
            log.warn("[BookingService] Spot not found: spotId={}, status={}", request.getSpotId(), e.status());
            throw new RuntimeException("Spot not found with id: " + request.getSpotId());
        } catch (FeignException.ServiceUnavailable e) {
            log.error("[BookingService] Spot service returned 503: {}", e.getMessage());
            throw new RuntimeException("Spot service unavailable. Please try again later.");
        } catch (FeignException e) {
            log.error("[BookingService] FeignException from spot-service - Status: {}, Message: {}",
                    e.status(), e.getMessage(), e);
            throw new RuntimeException("Spot service unavailable. Please try again later.");
        }

        // Spot must be AVAILABLE
        if (!"AVAILABLE".equalsIgnoreCase(spot.getStatus())) {
            throw new IllegalStateException(
                    "Spot " + spot.getSpotNumber() + " is not available. Current status: " + spot.getStatus());
        }

        // Vehicle type must match spot's vehicle type
        if (!spot.getVehicleType().equalsIgnoreCase(vehicle.getVehicleType())) {
            throw new IllegalArgumentException(
                    "Vehicle type " + vehicle.getVehicleType()
                    + " is not compatible with spot type " + spot.getVehicleType() + ".");
        }

        // EV check — if vehicle is EV, spot must support EV charging
        if (Boolean.TRUE.equals(vehicle.getIsEV()) && !Boolean.TRUE.equals(spot.getIsEVCharging())) {
            throw new IllegalArgumentException(
                    "EV vehicle requires an EV charging spot. Selected spot does not support EV charging.");
        }

        // Snapshot pricePerHour at booking time — never re-fetch at checkout
        BigDecimal pricePerHour = spot.getPricePerHour();
        UUID lotId = spot.getLotId();

        // ── Step 4: Map vehicleType string → local VehicleType enum ──────────
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(vehicle.getVehicleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown vehicle type: " + vehicle.getVehicleType());
        }

        // ── Step 5: Reserve or Occupy spot (based on booking type) ────────────
        boolean spotActioned = false;
        try {
            if (request.getBookingType() == BookingType.PRE_BOOKING) {
                spotServiceClient.reserveSpot(request.getSpotId());
            } else {
                spotServiceClient.occupySpot(request.getSpotId());
            }
            spotActioned = true;
        } catch (FeignException e) {
            throw new RuntimeException("Failed to reserve spot. Spot service unavailable or spot already taken.");
        }

        // ── Step 6: Decrement available spots counter in lot ──────────────────
        try {
            parkingLotServiceClient.decrementAvailableSpots(lotId);
        } catch (FeignException e) {
            // ROLLBACK Step 5 — release the spot we just reserved/occupied
            if (spotActioned) {
                safeReleaseSpot(request.getSpotId());
            }
            throw new RuntimeException("Failed to update lot availability. Please try again.");
        }

        // ── Step 7: Build and save Booking entity ─────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        boolean isWalkIn = request.getBookingType() == BookingType.WALK_IN;

        Booking booking = Booking.builder()
                .userId(userId)
                .lotId(lotId)
                .spotId(request.getSpotId())
                .vehicleId(request.getVehicleId())
                .vehiclePlate(vehicle.getLicensePlate())
                .vehicleType(vehicleType)
                .bookingType(request.getBookingType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                // WALK_IN: checked in immediately; PRE_BOOKING: checkInTime is null until checkIn()
                .checkInTime(isWalkIn ? now : null)
                .checkOutTime(null)
                .status(isWalkIn ? BookingStatus.ACTIVE : BookingStatus.RESERVED)
                .totalAmount(null) // null until checkOut — never 0
                .pricePerHour(pricePerHour)
                .build();

        try {
            booking = bookingRepository.save(booking);
        } catch (Exception e) {
            // ROLLBACK Steps 5 & 6 — release spot + restore lot counter
            safeReleaseSpot(request.getSpotId());
            safeIncrementLot(lotId);
            throw new RuntimeException("Failed to save booking. All changes have been rolled back.");
        }

        log.info("[BookingService] Booking created: bookingId={}, userId={}, spotId={}, type={}",
                booking.getBookingId(), userId, request.getSpotId(), request.getBookingType());

        // ── Step 8: Publish event (fire-and-forget — never fails the booking) ─
        BookingResponse response = toResponse(booking);
        bookingEventPublisher.publishBookingCreated(response);

        // ── Step 9: Return 201 CREATED payload ────────────────────────────────
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CHECK-IN  (RESERVED → ACTIVE)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public BookingResponse checkIn(UUID bookingId, UUID userId) {

        // Step 1: Fetch booking
        Booking booking = findBookingOrThrow(bookingId);

        // Step 2: Ownership check
        enforceOwnership(booking, userId);

        // Step 3: Must be RESERVED to check in
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot check in. Booking status is " + booking.getStatus()
                    + ". Only RESERVED bookings can be checked in.");
        }

        // Step 4: Must be a PRE_BOOKING — WALK_IN is already checked in at creation
        if (booking.getBookingType() == BookingType.WALK_IN) {
            throw new IllegalArgumentException(
                    "WALK_IN bookings are automatically checked in at creation. No manual check-in required.");
        }

        // Step 5: Occupy the spot in spot-service
        try {
            spotServiceClient.occupySpot(booking.getSpotId());
        } catch (FeignException e) {
            throw new RuntimeException("Failed to occupy spot. Spot service unavailable.");
        }

        // Step 6 & 7: Update and save
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        log.info("[BookingService] Check-in: bookingId={}, userId={}, spotId={}",
                bookingId, userId, booking.getSpotId());

        // Step 8: Publish event
        BookingResponse response = toResponse(booking);
        bookingEventPublisher.publishCheckIn(response);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CHECK-OUT  (ACTIVE → COMPLETED)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public BookingResponse checkOut(UUID bookingId, UUID userId) {

        // Step 1: Fetch booking
        Booking booking = findBookingOrThrow(bookingId);

        // Step 2: Ownership check — DRIVER checks own booking, MANAGER/ADMIN handled in controller
        // Service-level check: userId must match unless controller already verified role
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You are not authorized to check out this booking.");
        }

        // Step 3: Must be ACTIVE to check out
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot check out. Booking status is " + booking.getStatus()
                    + ". Only ACTIVE bookings can be checked out.");
        }

        // Step 4: Set checkout time
        LocalDateTime checkOutTime = LocalDateTime.now();
        booking.setCheckOutTime(checkOutTime);

        // Step 5: Calculate fare ──────────────────────────────────────────────
        // Duration from actual checkInTime to now
        // Minimum billing: 1 hour (even if parked for 10 minutes)
        BigDecimal totalAmount = computeFare(booking.getCheckInTime(), checkOutTime, booking.getPricePerHour());
        booking.setTotalAmount(totalAmount);

        // Step 6: Release spot
        try {
            spotServiceClient.releaseSpot(booking.getSpotId());
        } catch (FeignException e) {
            // Log but proceed — spot release failure should not block checkout
            // Spot can be manually reconciled; driver should not be stuck
            log.error("[BookingService] Failed to release spot {} during checkout for bookingId={}: {}",
                    booking.getSpotId(), bookingId, e.getMessage());
        }

        // Step 7: Increment lot counter
        try {
            parkingLotServiceClient.incrementAvailableSpots(booking.getLotId());
        } catch (FeignException e) {
            log.error("[BookingService] Failed to increment lot {} counter during checkout for bookingId={}: {}",
                    booking.getLotId(), bookingId, e.getMessage());
        }

        // Step 8 & 9: Update status and save
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        log.info("[BookingService] Check-out: bookingId={}, totalAmount={}, duration={}min",
                bookingId, totalAmount,
                Duration.between(booking.getCheckInTime(), checkOutTime).toMinutes());

        // Step 10: Publish event — payment-service listens to booking.checkout
        BookingResponse response = toResponse(booking);
        bookingEventPublisher.publishCheckOut(response);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CANCEL BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, String requesterRole) {

        // Step 1: Fetch booking
        Booking booking = findBookingOrThrow(bookingId);

        // Step 2: Role-based access control
        switch (requesterRole.toUpperCase()) {
            case "DRIVER":
                // Drivers can only cancel their own bookings
                if (!booking.getUserId().equals(userId)) {
                    throw new SecurityException("You can only cancel your own bookings.");
                }
                break;
            case "MANAGER":
                // Managers can cancel bookings at their lots
                // Note: lot ownership check can be added here if parkinglot-service
                // exposes a /lots/{lotId}/owner endpoint. For now, MANAGER can cancel
                // any booking (further restriction can be added in next iteration).
                break;
            case "ADMIN":
                // Admin can cancel any booking — no restriction
                break;
            default:
                throw new SecurityException("Unrecognized role: " + requesterRole);
        }

        // Step 3: Can only cancel RESERVED or ACTIVE bookings
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a COMPLETED booking.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already CANCELLED.");
        }

        // Step 4: Release spot
        safeReleaseSpot(booking.getSpotId());

        // Step 5: Increment lot counter
        safeIncrementLot(booking.getLotId());

        // Step 6 & 7: Update status and save
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        log.info("[BookingService] Booking cancelled: bookingId={}, cancelledBy={}, role={}",
                bookingId, userId, requesterRole);

        // Step 8: Publish cancellation event
        BookingResponse response = toResponse(booking);
        bookingEventPublisher.publishCancellation(response);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. EXTEND BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public BookingResponse extendBooking(UUID bookingId, UUID userId, ExtendBookingRequest request) {

        // Step 1: Fetch booking
        Booking booking = findBookingOrThrow(bookingId);

        // Step 2: Ownership check
        enforceOwnership(booking, userId);

        // Step 3: Can only extend RESERVED or ACTIVE bookings
        if (booking.getStatus() != BookingStatus.RESERVED && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot extend booking with status " + booking.getStatus()
                    + ". Only RESERVED or ACTIVE bookings can be extended.");
        }

        // Step 4: newEndTime must be strictly after current endTime
        if (!request.getNewEndTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException(
                    "newEndTime must be after the current endTime (" + booking.getEndTime() + ").");
        }

        // Step 5: newEndTime must not be in the past
        if (request.getNewEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("newEndTime cannot be in the past.");
        }

        // Step 6 & 7: Update and save
        booking.setEndTime(request.getNewEndTime());
        booking = bookingRepository.save(booking);

        log.info("[BookingService] Booking extended: bookingId={}, newEndTime={}",
                bookingId, request.getNewEndTime());

        // Step 8: Publish extension event
        BookingResponse response = toResponse(booking);
        bookingEventPublisher.publishExtension(response);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. GET BOOKING BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId) {
        return toResponse(findBookingOrThrow(bookingId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. GET BOOKINGS BY USER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUser(UUID userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. GET BOOKINGS BY LOT
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByLot(UUID lotId) {
        return bookingRepository.findByLotId(lotId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. GET ACTIVE BOOKINGS FOR LOT
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getActiveBookings(UUID lotId) {
        return bookingRepository.findByLotIdAndStatus(lotId, BookingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. GET BOOKING HISTORY (COMPLETED + CANCELLED) FOR USER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingHistory(UUID userId) {
        // Use ordered query — newest first
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED
                || b.getStatus() == BookingStatus.CANCELLED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. GET ALL BOOKINGS (ADMIN ONLY)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. CALCULATE FARE ESTIMATE (READ-ONLY)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public FareCalculationResponse calculateAmount(UUID bookingId) {

        // Step 1: Fetch booking
        Booking booking = findBookingOrThrow(bookingId);

        // Step 2: Must be ACTIVE — needs a checkInTime to calculate from
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Fare estimate is only available for ACTIVE bookings. "
                    + "Current status: " + booking.getStatus());
        }

        // Step 3 & 4: Estimate based on current time
        LocalDateTime now = LocalDateTime.now();
        double rawHours = Duration.between(booking.getCheckInTime(), now).toMinutes() / 60.0;

        // Step 5: Minimum 1 hour billing
        double billableHours = Math.max(1.0, rawHours);
        BigDecimal estimatedHoursBD = BigDecimal.valueOf(billableHours).setScale(2, RoundingMode.HALF_UP);

        // Step 6: Compute estimate
        BigDecimal estimatedFare = booking.getPricePerHour()
                .multiply(estimatedHoursBD)
                .setScale(2, RoundingMode.HALF_UP);

        return FareCalculationResponse.builder()
                .bookingId(booking.getBookingId())
                .pricePerHour(booking.getPricePerHour())
                .estimatedHours(estimatedHoursBD)
                .estimatedFare(estimatedFare)
                .checkInTime(booking.getCheckInTime())
                .calculatedAt(now)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. AUTO-EXPIRE BOOKINGS (CALLED BY SCHEDULER — NO JWT CONTEXT)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void autoExpireBookings() {

        // Step 1: Compute the expiry threshold
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(gracePeriodMinutes);

        // Step 2: Find all RESERVED PRE_BOOKINGs whose startTime passed the threshold
        List<Booking> expired = bookingRepository.findExpiredPreBookings(expiryThreshold);

        if (expired.isEmpty()) {
            log.debug("[Scheduler] No expired bookings found at threshold={}", expiryThreshold);
            return;
        }

        log.info("[Scheduler] Found {} expired PRE_BOOKING(s) to auto-cancel.", expired.size());

        int successCount = 0;

        for (Booking booking : expired) {
            try {
                // Step 3a: Release spot
                safeReleaseSpot(booking.getSpotId());

                // Step 3b: Restore lot counter
                safeIncrementLot(booking.getLotId());

                // Step 3c: Cancel booking
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                // Step 3d: Publish cancellation event
                bookingEventPublisher.publishCancellation(toResponse(booking));

                successCount++;

                log.info("[Scheduler] Auto-cancelled bookingId={}, userId={}, spotId={}",
                        booking.getBookingId(), booking.getUserId(), booking.getSpotId());

            } catch (Exception e) {
                // Per-booking failure must not abort the entire batch
                log.error("[Scheduler] Failed to auto-cancel bookingId={}: {}",
                        booking.getBookingId(), e.getMessage());
            }
        }

        log.info("[Scheduler] Auto-expiry complete. Cancelled {}/{} expired bookings.",
                successCount, expired.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Central mapper — Booking entity → BookingResponse DTO. Called by every
     * service method before returning. vehicleType stored as enum, returned as
     * String for API consumers.
     */
    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUserId())
                .lotId(booking.getLotId())
                .spotId(booking.getSpotId())
                .vehicleId(booking.getVehicleId())
                .vehiclePlate(booking.getVehiclePlate())
                .vehicleType(booking.getVehicleType() != null
                        ? booking.getVehicleType().name() : null)
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .checkInTime(booking.getCheckInTime()) // null for RESERVED PRE_BOOKING
                .checkOutTime(booking.getCheckOutTime()) // null until checkOut
                .pricePerHour(booking.getPricePerHour())
                .totalAmount(booking.getTotalAmount()) // null until checkOut
                .createdAt(booking.getCreatedAt())
                .build();
    }

    /**
     * Centralized fare computation used by both checkOut() and
     * calculateAmount(). Enforces minimum 1-hour billing. Rounds to 2 decimal
     * places (HALF_UP) — monetary precision.
     */
    private BigDecimal computeFare(LocalDateTime checkIn, LocalDateTime checkOut,
            BigDecimal pricePerHour) {
        double rawMinutes = Duration.between(checkIn, checkOut).toMinutes();
        double rawHours = rawMinutes / 60.0;
        double billableHours = Math.max(1.0, rawHours);

        return pricePerHour
                .multiply(BigDecimal.valueOf(billableHours))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fetch booking by UUID — throws 404-mapped RuntimeException if absent.
     * Used by every lifecycle method as the first step.
     */
    private Booking findBookingOrThrow(UUID bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException(
                "Booking not found with id: " + bookingId));
    }

    /**
     * Ownership guard — throws 403-mapped SecurityException if userId mismatch.
     * Used by checkIn(), extendBooking(), and cancelBooking() (DRIVER branch).
     */
    private void enforceOwnership(Booking booking, UUID userId) {
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException(
                    "You are not authorized to perform this action on booking: "
                    + booking.getBookingId());
        }
    }

    /**
     * Safe spot release — logs error instead of throwing. Used in rollback
     * paths and auto-expiry where a failure must not abort the parent
     * transaction or batch loop.
     */
    private void safeReleaseSpot(UUID spotId) {
        try {
            spotServiceClient.releaseSpot(spotId);
        } catch (Exception e) {
            log.error("[Rollback] Failed to release spotId={}: {}", spotId, e.getMessage());
        }
    }

    /**
     * Safe lot counter increment — logs error instead of throwing. Used in
     * rollback paths and auto-expiry.
     */
    private void safeIncrementLot(UUID lotId) {
        try {
            parkingLotServiceClient.incrementAvailableSpots(lotId);
        } catch (Exception e) {
            log.error("[Rollback] Failed to increment lot counter for lotId={}: {}",
                    lotId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYTICS — Booking Statistics
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats(LocalDateTime from, LocalDateTime to) {
        log.info("[Analytics] Fetching booking stats for period: {} to {}", from, to);

        // Count ACTIVE bookings (current state, regardless of date range)
        long activeCount = bookingRepository.countByStatus(BookingStatus.ACTIVE);

        // Count COMPLETED bookings within the date range
        long completedCount = bookingRepository.findByStatus(BookingStatus.COMPLETED).stream()
                .filter(b -> b.getCreatedAt() != null
                && !b.getCreatedAt().isBefore(from) && !b.getCreatedAt().isAfter(to))
                .count();

        // Count CANCELLED bookings within the date range
        long cancelledCount = bookingRepository.findByStatus(BookingStatus.CANCELLED).stream()
                .filter(b -> b.getCreatedAt() != null
                && !b.getCreatedAt().isBefore(from) && !b.getCreatedAt().isAfter(to))
                .count();

        log.info("[Analytics] Booking stats - active: {}, completed: {}, cancelled: {}",
                activeCount, completedCount, cancelledCount);

        return BookingStatsResponse.builder()
                .activeBookings(activeCount)
                .completedBookings(completedCount)
                .cancelledBookings(cancelledCount)
                .periodStart(from)
                .periodEnd(to)
                .computedAt(LocalDateTime.now())
                .build();
    }
}
