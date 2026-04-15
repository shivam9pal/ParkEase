package com.parkease.analytics.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkease.analytics.dto.AvgDurationResponse;
import com.parkease.analytics.dto.DailyReportResponse;
import com.parkease.analytics.dto.HourlyOccupancyResponse;
import com.parkease.analytics.dto.OccupancyRateResponse;
import com.parkease.analytics.dto.PeakHourResponse;
import com.parkease.analytics.dto.PlatformOccupancyResponse;
import com.parkease.analytics.dto.PlatformSummaryResponse;
import com.parkease.analytics.dto.SpotTypeUtilisationResponse;
import com.parkease.analytics.entity.EventType;
import com.parkease.analytics.entity.OccupancyLog;
import com.parkease.analytics.enums.Period;
import com.parkease.analytics.exception.ResourceNotFoundException;
import com.parkease.analytics.feign.ParkingLotServiceClient;
import com.parkease.analytics.feign.PaymentServiceClient;
import com.parkease.analytics.feign.SpotServiceClient;
import com.parkease.analytics.feign.UserServiceClient;
import com.parkease.analytics.feign.BookingServiceClient;
import com.parkease.analytics.feign.dto.DailyRevenueDto;
import com.parkease.analytics.feign.dto.LotSummaryDto;
import com.parkease.analytics.feign.dto.RevenueDto;
import com.parkease.analytics.feign.dto.BookingStatsDto;
import com.parkease.analytics.rabbitmq.dto.BookingEventPayload;
import com.parkease.analytics.repository.OccupancyLogRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OccupancyLogRepository occupancyLogRepository;
    private final ParkingLotServiceClient parkingLotServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final SpotServiceClient spotServiceClient;
    private final UserServiceClient userServiceClient;
    private final BookingServiceClient bookingServiceClient;

    // ═══════════════════════════════════════════════════════════════════
    // SECURITY — Manager Ownership Enforcement
    // ═══════════════════════════════════════════════════════════════════
    /**
     * If the caller is a MANAGER, verifies they own the requested lot. ADMINs
     * bypass this check entirely — they can access all lots. Throws
     * AccessDeniedException (→ 403) if ownership check fails.
     */
    private void enforceManagerOwnership(UUID lotId, UUID requesterId, String role) {
        log.info("[SECURITY] Access check - lotId: {}, requesterId: {}, role: {}", lotId, requesterId, role);

        if ("MANAGER".equals(role)) {
            log.info("[SECURITY] User is MANAGER - checking lot ownership");
            LotSummaryDto lot = safeGetLot(lotId);

            if (lot == null) {
                log.error("[SECURITY] Lot not found: {}", lotId);
                throw new AccessDeniedException("Lot not found: " + lotId);
            }

            log.info("[SECURITY] Lot found. ManagerId: {}, RequesterId: {}", lot.getManagerId(), requesterId);

            if (!requesterId.equals(lot.getManagerId())) {
                log.warn("[SECURITY] ACCESS DENIED - MANAGER userId={} tried to access lotId={} owned by managerId={}",
                        requesterId, lotId, lot.getManagerId());
                throw new AccessDeniedException(
                        "Access denied: lot does not belong to this manager (you: " + requesterId + ", owner: " + lot.getManagerId() + ")");
            }
            log.info("[SECURITY] Ownership check passed for MANAGER");
        } else if ("ADMIN".equals(role)) {
            log.info("[SECURITY] User is ADMIN - bypassing ownership check");
        } else {
            log.warn("[SECURITY] Unknown role: {}. Access may be blocked by @PreAuthorize", role);
        }
    }

    /**
     * Extracts userId UUID from SecurityContextHolder (set by JwtAuthFilter).
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map<?, ?> details) {
            Object userId = details.get("userId");
            return (userId instanceof UUID) ? (UUID) userId : null;
        }
        return null;
    }

    /**
     * Extracts role string (e.g. "MANAGER" / "ADMIN") from
     * SecurityContextHolder.
     */
    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map<?, ?> details) {
            Object role = details.get("role");
            return (role instanceof String) ? (String) role : "";
        }
        return "";
    }

    // ═══════════════════════════════════════════════════════════════════
    // EVENT PROCESSING  (RabbitMQ consumer — no HTTP context, no check)
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void processBookingEvent(BookingEventPayload payload, String routingKey) {
        String eventType = switch (routingKey) {
            case "booking.created" ->
                EventType.BOOKING_CREATED;
            case "booking.checkin" ->
                EventType.CHECKIN;
            case "booking.checkout" ->
                EventType.CHECKOUT;
            case "booking.cancelled" ->
                EventType.CANCELLED;
            case "booking.extended" ->
                null;
            default -> {
                log.warn("Unhandled booking routing key: {}", routingKey);
                yield null;
            }
        };

        if (eventType == null) {
            return;
        }

        LotSummaryDto lot = safeGetLot(payload.getLotId());

        String spotType = null;
        if (EventType.BOOKING_CREATED.equals(eventType) && payload.getSpotId() != null) {
            spotType = safeGetSpotType(payload.getSpotId());
        }

        Long durationMinutes = null;
        if (EventType.CHECKOUT.equals(eventType)
                && payload.getCheckInTime() != null
                && payload.getCheckOutTime() != null) {
            durationMinutes = Duration.between(
                    payload.getCheckInTime(), payload.getCheckOutTime()).toMinutes();
            durationMinutes = Math.max(60L, durationMinutes);
        }

        Double occupancyRate = null;
        Integer availableSpots = null;
        Integer totalSpots = null;
        if (lot != null && lot.getTotalSpots() != null && lot.getTotalSpots() > 0) {
            availableSpots = lot.getAvailableSpots();
            totalSpots = lot.getTotalSpots();
            occupancyRate = ((double) (totalSpots - availableSpots) / totalSpots) * 100.0;
            occupancyRate = Math.round(occupancyRate * 100.0) / 100.0;
        }

        OccupancyLog entry = OccupancyLog.builder()
                .lotId(payload.getLotId())
                .spotId(payload.getSpotId())
                .spotType(spotType)
                .vehicleType(payload.getVehicleType())
                .eventType(eventType)
                .occupancyRate(occupancyRate)
                .availableSpots(availableSpots)
                .totalSpots(totalSpots)
                .checkInTime(payload.getCheckInTime())
                .checkOutTime(payload.getCheckOutTime())
                .durationMinutes(durationMinutes)
                .build();

        occupancyLogRepository.save(entry);
        log.debug("Saved OccupancyLog: lotId={}, eventType={}", payload.getLotId(), eventType);
    }

    // ═══════════════════════════════════════════════════════════════════
    // SCHEDULER  (system thread — no HTTP context, no check)
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void logScheduledSnapshots() {
        List<LotSummaryDto> lots = safeGetAllLots();
        int count = 0;

        for (LotSummaryDto lot : lots) {
            if (!Boolean.TRUE.equals(lot.getIsApproved())) {
                continue;
            }

            Double occupancyRate = null;
            if (lot.getTotalSpots() != null && lot.getTotalSpots() > 0) {
                occupancyRate = ((double) (lot.getTotalSpots() - lot.getAvailableSpots())
                        / lot.getTotalSpots()) * 100.0;
                occupancyRate = Math.round(occupancyRate * 100.0) / 100.0;
            }

            OccupancyLog snapshot = OccupancyLog.builder()
                    .lotId(lot.getLotId())
                    .spotId(null).spotType(null).vehicleType(null)
                    .eventType(EventType.SCHEDULED)
                    .occupancyRate(occupancyRate)
                    .availableSpots(lot.getAvailableSpots())
                    .totalSpots(lot.getTotalSpots())
                    .checkInTime(null).checkOutTime(null).durationMinutes(null)
                    .build();

            occupancyLogRepository.save(snapshot);
            count++;
        }
        log.info("Scheduled snapshot complete: {} approved lots logged", count);
    }

    // ═══════════════════════════════════════════════════════════════════
    // OCCUPANCY METRICS  — all protected by enforceManagerOwnership
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public OccupancyRateResponse getOccupancyRate(UUID lotId) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        Optional<OccupancyLog> latest = occupancyLogRepository
                .findFirstByLotIdAndEventTypeOrderByTimestampDesc(lotId, EventType.SCHEDULED);

        if (latest.isEmpty()) {
            LotSummaryDto lot = safeGetLot(lotId);
            if (lot == null) {
                throw new ResourceNotFoundException("Lot not found: " + lotId);
            }
            double rate = lot.getTotalSpots() > 0
                    ? ((double) (lot.getTotalSpots() - lot.getAvailableSpots()) / lot.getTotalSpots()) * 100.0
                    : 0.0;
            return OccupancyRateResponse.builder()
                    .lotId(lotId)
                    .occupancyRate(Math.round(rate * 100.0) / 100.0)
                    .availableSpots(lot.getAvailableSpots())
                    .totalSpots(lot.getTotalSpots())
                    .computedAt(LocalDateTime.now())
                    .build();
        }

        OccupancyLog entry = latest.get();
        return OccupancyRateResponse.builder()
                .lotId(lotId)
                .occupancyRate(entry.getOccupancyRate() != null ? entry.getOccupancyRate() : 0.0)
                .availableSpots(entry.getAvailableSpots())
                .totalSpots(entry.getTotalSpots())
                .computedAt(entry.getTimestamp())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyOccupancyResponse> getOccupancyByHour(UUID lotId) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        LocalDateTime from = LocalDateTime.now().minusDays(30);
        List<OccupancyLog> logs = occupancyLogRepository
                .findByLotIdAndEventTypeAndTimestampAfter(lotId, EventType.SCHEDULED, from);

        Map<Integer, Double> avgByHour = logs.stream()
                .filter(l -> l.getOccupancyRate() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().getHour(),
                        Collectors.averagingDouble(OccupancyLog::getOccupancyRate)
                ));

        return IntStream.range(0, 24)
                .mapToObj(hour -> HourlyOccupancyResponse.builder()
                .hour(hour)
                .averageOccupancyRate(
                        Math.round(avgByHour.getOrDefault(hour, 0.0) * 100.0) / 100.0)
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeakHourResponse> getPeakHours(UUID lotId, int topN) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        int clampedTopN = Math.min(topN, 24);
        // NOTE: getOccupancyByHour would double-check ownership — call repo directly here
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        List<OccupancyLog> logs = occupancyLogRepository
                .findByLotIdAndEventTypeAndTimestampAfter(lotId, EventType.SCHEDULED, from);

        Map<Integer, Double> avgByHour = logs.stream()
                .filter(l -> l.getOccupancyRate() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().getHour(),
                        Collectors.averagingDouble(OccupancyLog::getOccupancyRate)
                ));

        return IntStream.range(0, 24)
                .mapToObj(hour -> HourlyOccupancyResponse.builder()
                .hour(hour)
                .averageOccupancyRate(
                        Math.round(avgByHour.getOrDefault(hour, 0.0) * 100.0) / 100.0)
                .build())
                .sorted(Comparator.comparingDouble(HourlyOccupancyResponse::getAverageOccupancyRate)
                        .reversed())
                .limit(clampedTopN)
                .map(h -> PeakHourResponse.builder()
                .hour(h.getHour())
                .averageOccupancyRate(h.getAverageOccupancyRate())
                .label(String.format("%02d:00 - %02d:00", h.getHour(), (h.getHour() + 1) % 24))
                .build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // REVENUE  — delegated to payment-service, still ownership-checked
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public RevenueDto getLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒
        return safeGetLotRevenue(lotId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenueDto> getLotDailyRevenue(UUID lotId, LocalDateTime from, LocalDateTime to) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒
        try {
            return paymentServiceClient.getLotDailyRevenue(lotId, from, to);
        } catch (FeignException e) {
            log.error("payment-service unavailable for daily revenue lotId={}: {}", lotId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILISATION & DURATION
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public List<SpotTypeUtilisationResponse> getSpotTypeUtilisation(UUID lotId) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        List<OccupancyLog> logs = occupancyLogRepository
                .findByLotIdAndEventType(lotId, EventType.BOOKING_CREATED);

        Map<String, Long> countByType = logs.stream()
                .filter(l -> l.getSpotType() != null)
                .collect(Collectors.groupingBy(OccupancyLog::getSpotType, Collectors.counting()));

        long total = countByType.values().stream().mapToLong(Long::longValue).sum();

        return countByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> SpotTypeUtilisationResponse.builder()
                .spotType(entry.getKey())
                .bookingCount(entry.getValue())
                .percentage(total > 0
                        ? Math.round((entry.getValue() * 100.0 / total) * 100.0) / 100.0
                        : 0.0)
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AvgDurationResponse getAvgDuration(UUID lotId) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        Double avg = occupancyLogRepository.avgDurationByLotId(lotId);
        return AvgDurationResponse.builder()
                .lotId(lotId)
                .averageDurationMinutes(avg != null ? Math.round(avg) : 0L)
                .averageDurationFormatted(formatDuration(avg != null ? avg.longValue() : 0L))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // PLATFORM SUMMARY  — ADMIN only (SecurityConfig enforces, no extra check)
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public PlatformSummaryResponse getPlatformSummary() {
        // No ownership check — SecurityConfig already restricts to ADMIN only
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        log.info("[DEBUG] Fetching platform summary. Date range - From: {} (start of today), To: {} (now)", todayStart, now);

        // Get ALL lots to count pending ones
        List<LotSummaryDto> allLots = safeGetAllLots();
        List<LotSummaryDto> approvedLots = allLots.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsApproved()))
                .collect(Collectors.toList());

        long pendingLots = allLots.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsApproved()))
                .count();
        log.info("[DEBUG] Lot summary: approved={}, pending={}", approvedLots.size(), pendingLots);

        int totalSpots = approvedLots.stream()
                .mapToInt(l -> l.getTotalSpots() != null ? l.getTotalSpots() : 0).sum();
        int totalAvailable = approvedLots.stream()
                .mapToInt(l -> l.getAvailableSpots() != null ? l.getAvailableSpots() : 0).sum();

        double platformOccupancyRate = totalSpots > 0
                ? Math.round(((double) (totalSpots - totalAvailable) / totalSpots) * 10000.0) / 100.0
                : 0.0;

        // Revenue data
        RevenueDto todayRevenue = safeGetPlatformRevenue(todayStart, now);
        Double platformAvgDuration = occupancyLogRepository.avgDurationPlatform();

        // Fetch user count (all DRIVER + MANAGER roles)
        Long totalUsers = 0L;
        try {
            // Note: getAllUsers returns a list, so we get the count
            // We make two calls: one for DRIVER, one for MANAGER to sum them up
            totalUsers = safeGetUserCount();
            log.info("[DEBUG] Total users in system: {}", totalUsers);
        } catch (Exception e) {
            log.warn("[DEBUG] Failed to fetch user count: {}", e.getMessage());
            totalUsers = 0L;
        }

        // Fetch booking statistics
        BookingStatsDto bookingStats = null;
        try {
            bookingStats = bookingServiceClient.getBookingStats(todayStart, now);
            log.info("[DEBUG] Booking stats: active={}, completed={}, cancelled={}",
                    bookingStats.getActiveBookings(),
                    bookingStats.getCompletedBookings(),
                    bookingStats.getCancelledBookings());
        } catch (FeignException e) {
            log.warn("[DEBUG] Failed to fetch booking stats (status={}): {}",
                    e.status(), e.contentUTF8());
            bookingStats = BookingStatsDto.builder()
                    .activeBookings(0)
                    .completedBookings(0)
                    .cancelledBookings(0)
                    .periodStart(todayStart)
                    .periodEnd(now)
                    .computedAt(now)
                    .build();
        } catch (Exception e) {
            log.warn("[DEBUG] Failed to fetch booking stats: {}", e.getMessage());
            bookingStats = BookingStatsDto.builder()
                    .activeBookings(0)
                    .completedBookings(0)
                    .cancelledBookings(0)
                    .periodStart(todayStart)
                    .periodEnd(now)
                    .computedAt(now)
                    .build();
        }

        return PlatformSummaryResponse.builder()
                .totalLots(approvedLots.size())
                .totalSpots(totalSpots)
                .totalAvailableSpots(totalAvailable)
                .platformOccupancyRate(platformOccupancyRate)
                .todayRevenue(todayRevenue != null ? todayRevenue.getTotalRevenue() : BigDecimal.ZERO)
                .todayTransactionCount(todayRevenue != null ? todayRevenue.getTransactionCount() : 0L)
                .platformAvgDurationMinutes(platformAvgDuration != null
                        ? Math.round(platformAvgDuration) : 0L)
                .totalUsers(totalUsers)
                .pendingLots(pendingLots)
                .activeBookings(bookingStats.getActiveBookings())
                .completedBookings(bookingStats.getCompletedBookings())
                .cancelledBookings(bookingStats.getCancelledBookings())
                .generatedAt(now)
                .build();
    }

    // ─── Platform Occupancy (ADMIN only) ───────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PlatformOccupancyResponse getPlatformOccupancy(Period period) {
        log.info("Fetching occupancy data for period: {}", period);

        List<LotSummaryDto> approvedLots = safeGetAllLots().stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsApproved()))
                .collect(Collectors.toList());

        int totalSpots = approvedLots.stream()
                .mapToInt(l -> l.getTotalSpots() != null ? l.getTotalSpots() : 0).sum();

        // Calculate date range based on period
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case DAILY:
                startDate = LocalDate.now().atStartOfDay();
                break;
            case WEEKLY:
                startDate = LocalDate.now().minusDays(7).atStartOfDay();
                break;
            case MONTHLY:
                startDate = LocalDate.now().minusDays(30).atStartOfDay();
                break;
            default:
                startDate = LocalDate.now().atStartOfDay();
        }

        // Get hourly breakdown
        List<HourlyOccupancyResponse> hourlyBreakdown = IntStream.range(0, 24)
                .mapToObj(hour -> HourlyOccupancyResponse.builder()
                .hour(hour)
                .averageOccupancyRate(0.0)
                .build())
                .collect(Collectors.toList());

        // Calculate average occupancy rate for the period across all lots
        List<OccupancyLog> periodLogs = occupancyLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(startDate, now);

        Double avgOccupancy = 0.0;
        Double peakOccupancy = 0.0;
        Double minOccupancy = 100.0;
        Integer avgAvailableSpots = 0;

        if (!periodLogs.isEmpty()) {
            avgOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .average()
                    .orElse(0.0);

            peakOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .max()
                    .orElse(0.0);

            minOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .min()
                    .orElse(0.0);

            avgAvailableSpots = (int) periodLogs.stream()
                    .mapToInt(log -> log.getAvailableSpots() != null ? log.getAvailableSpots() : 0)
                    .average()
                    .orElse(0);
        }

        return PlatformOccupancyResponse.builder()
                .averageOccupancyRate(Math.round(avgOccupancy * 100.0) / 100.0)
                .peakOccupancyRate(Math.round(peakOccupancy * 100.0) / 100.0)
                .minOccupancyRate(Math.round(minOccupancy * 100.0) / 100.0)
                .totalLotsAnalyzed(approvedLots.size())
                .totalSpots(totalSpots)
                .averageAvailableSpots(avgAvailableSpots)
                .period(period.name())
                .hourlyBreakdown(hourlyBreakdown)
                .computedAt(now)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // DAILY REPORT
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(UUID lotId) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole()); // 🔒

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // Call repo directly (ownership already checked above — avoid double Feign call)
        Optional<OccupancyLog> latestLog = occupancyLogRepository
                .findFirstByLotIdAndEventTypeOrderByTimestampDesc(lotId, EventType.SCHEDULED);

        double currentOccupancy = latestLog.map(l
                -> l.getOccupancyRate() != null ? l.getOccupancyRate() : 0.0).orElse(0.0);
        Integer availableSpots = latestLog.map(OccupancyLog::getAvailableSpots).orElse(null);
        Integer totalSpots = latestLog.map(OccupancyLog::getTotalSpots).orElse(null);

        // Peak hours (direct repo — ownership already checked)
        LocalDateTime lookback = LocalDateTime.now().minusDays(30);
        List<OccupancyLog> scheduledLogs = occupancyLogRepository
                .findByLotIdAndEventTypeAndTimestampAfter(lotId, EventType.SCHEDULED, lookback);
        Map<Integer, Double> avgByHour = scheduledLogs.stream()
                .filter(l -> l.getOccupancyRate() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().getHour(),
                        Collectors.averagingDouble(OccupancyLog::getOccupancyRate)));
        List<PeakHourResponse> peakHours = IntStream.range(0, 24)
                .mapToObj(h -> PeakHourResponse.builder()
                .hour(h)
                .averageOccupancyRate(Math.round(avgByHour.getOrDefault(h, 0.0) * 100.0) / 100.0)
                .label(String.format("%02d:00 - %02d:00", h, (h + 1) % 24))
                .build())
                .sorted(Comparator.comparingDouble(PeakHourResponse::getAverageOccupancyRate).reversed())
                .limit(3)
                .collect(Collectors.toList());

        RevenueDto revenue = safeGetLotRevenue(lotId, todayStart, now);

        Double avgDur = occupancyLogRepository.avgDurationByLotId(lotId);
        long avgDurMinutes = avgDur != null ? Math.round(avgDur) : 0L;

        List<OccupancyLog> bookingLogs = occupancyLogRepository
                .findByLotIdAndEventType(lotId, EventType.BOOKING_CREATED);
        Map<String, Long> countByType = bookingLogs.stream()
                .filter(l -> l.getSpotType() != null)
                .collect(Collectors.groupingBy(OccupancyLog::getSpotType, Collectors.counting()));
        long total = countByType.values().stream().mapToLong(Long::longValue).sum();
        List<SpotTypeUtilisationResponse> spotTypes = countByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> SpotTypeUtilisationResponse.builder()
                .spotType(e.getKey()).bookingCount(e.getValue())
                .percentage(total > 0
                        ? Math.round(e.getValue() * 100.0 / total * 100.0) / 100.0 : 0.0)
                .build())
                .collect(Collectors.toList());

        long todayBookings = occupancyLogRepository.countByLotIdAndEventTypeAndTimestampBetween(
                lotId, EventType.BOOKING_CREATED, todayStart, now);
        long todayCheckouts = occupancyLogRepository.countByLotIdAndEventTypeAndTimestampBetween(
                lotId, EventType.CHECKOUT, todayStart, now);

        return DailyReportResponse.builder()
                .lotId(lotId)
                .reportDate(LocalDate.now())
                .currentOccupancyRate(currentOccupancy)
                .availableSpots(availableSpots)
                .totalSpots(totalSpots)
                .peakHours(peakHours)
                .todayRevenue(revenue != null ? revenue.getTotalRevenue() : BigDecimal.ZERO)
                .todayTransactionCount(revenue != null ? revenue.getTransactionCount() : 0L)
                .todayBookingsCreated(todayBookings)
                .todayCheckouts(todayCheckouts)
                .averageParkingDurationMinutes(avgDurMinutes)
                .averageParkingDurationFormatted(formatDuration(avgDurMinutes))
                .spotTypeUtilisation(spotTypes)
                .generatedAt(now)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // SAFE FEIGN HELPERS
    // ═══════════════════════════════════════════════════════════════════
    private LotSummaryDto safeGetLot(UUID lotId) {
        try {
            return parkingLotServiceClient.getLotById(lotId);
        } catch (FeignException.NotFound e) {
            log.warn("Lot not found in parkinglot-service: lotId={}", lotId);
            return null;
        } catch (FeignException e) {
            log.error("parkinglot-service unavailable for lotId={}: {}", lotId, e.getMessage());
            return null;
        }
    }

    private List<LotSummaryDto> safeGetAllLots() {
        try {
            return parkingLotServiceClient.getAllLots();
        } catch (FeignException e) {
            log.error("[CRITICAL] parkinglot-service unavailable for getAllLots. Status: {}, Body: {}",
                    e.status(), e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to fetch parking lots from parkinglot-service: " + e.getMessage(), e);
        }
    }

    private String safeGetSpotType(UUID spotId) {
        try {
            return spotServiceClient.getSpotById(spotId).getSpotType();
        } catch (FeignException.NotFound e) {
            log.warn("Spot not found in spot-service: spotId={}", spotId);
            return null;
        } catch (FeignException e) {
            log.error("spot-service unavailable for spotId={}: {}", spotId, e.getMessage());
            return null;
        }
    }

    private RevenueDto safeGetLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to) {
        try {
            return paymentServiceClient.getLotRevenue(lotId, from, to);
        } catch (FeignException e) {
            log.error("payment-service unavailable for revenue lotId={}: {}", lotId, e.getMessage());
            return null;
        }
    }

    private RevenueDto safeGetPlatformRevenue(LocalDateTime from, LocalDateTime to) {
        try {
            log.debug("Fetching platform revenue from payment-service. From: {}, To: {}", from, to);
            RevenueDto result = paymentServiceClient.getPlatformRevenue(from, to);
            log.debug("Platform revenue fetched successfully: {}", result);
            return result;
        } catch (FeignException.NotFound e) {
            log.warn("[WARNING] Payment endpoint not found (404). Status: {}", e.status());
            throw new RuntimeException("Payment service endpoint /api/v1/payments/revenue/platform not found. Check if endpoint exists and is accessible.", e);
        } catch (FeignException.Unauthorized e) {
            log.error("[CRITICAL] Unauthorized access to payment-service. Status: {}", e.status());
            throw new RuntimeException("Unauthorized to access payment-service. Check system JWT configuration.", e);
        } catch (FeignException e) {
            log.error("[CRITICAL] payment-service error while fetching platform revenue. Status: {}, Body: {}",
                    e.status(), e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to fetch platform revenue from payment-service: Status " + e.status() + " - " + e.getMessage(), e);
        }
    }

    private String formatDuration(long minutes) {
        return String.format("%dh %02dm", minutes / 60, minutes % 60);
    }

    private Long safeGetUserCount() {
        try {
            log.debug("Fetching all users from auth-service");
            // Fetch all users (calls /api/v1/auth/users which returns List<UserProfileResponse>)
            // We then count both DRIVER and MANAGER roles
            // Since the endpoint returns a list, we call it without role filter to get all users
            var userCountDto = userServiceClient.getAllUsers(null);
            long totalUsers = userCountDto.getUsers() != null ? userCountDto.getUsers().size() : 0;
            log.debug("Total users fetched: {}", totalUsers);
            return totalUsers;
        } catch (FeignException.Unauthorized e) {
            log.error("[CRITICAL] Unauthorized access to auth-service. Status: {}", e.status());
            throw new RuntimeException("Unauthorized to access auth-service. Check system JWT configuration.", e);
        } catch (FeignException e) {
            log.error("[CRITICAL] auth-service error while fetching users. Status: {}, Body: {}",
                    e.status(), e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to fetch users from auth-service: Status " + e.status() + " - " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.parkease.analytics.dto.LotSummaryAnalyticsResponse getLotAnalyticsSummary(UUID lotId) {
        log.info("[API] getLotAnalyticsSummary called for lotId: {}", lotId);

        UUID userId = getCurrentUserId();
        String role = getCurrentRole();
        log.info("[API] Current user - userId: {}, role: {}", userId, role);

        enforceManagerOwnership(lotId, userId, role);

        LotSummaryDto lot = safeGetLot(lotId);
        if (lot == null) {
            throw new ResourceNotFoundException("Lot not found: " + lotId);
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        RevenueDto todayRevenue = safeGetLotRevenue(lotId, todayStart, now);
        Double avgDuration = occupancyLogRepository.avgDurationByLot(lotId);
        Double peakOccupancy = occupancyLogRepository.peakOccupancyByLot(lotId);

        return com.parkease.analytics.dto.LotSummaryAnalyticsResponse.builder()
                .lotId(lotId)
                .lotName(lot.getName())
                .totalSpots(lot.getTotalSpots())
                .availableSpots(lot.getAvailableSpots())
                .currentOccupancyRate(lot.getTotalSpots() > 0
                        ? Math.round(((double) (lot.getTotalSpots() - lot.getAvailableSpots()) / lot.getTotalSpots()) * 10000.0) / 100.0
                        : 0.0)
                .todayRevenue(todayRevenue != null ? todayRevenue.getTotalRevenue() : BigDecimal.ZERO)
                .todayTransactionCount(todayRevenue != null ? todayRevenue.getTransactionCount() : 0L)
                .averageParkingDurationMinutes(avgDuration != null ? Math.round(avgDuration) : 0L)
                .peakOccupancyRate(peakOccupancy != null ? Math.round(peakOccupancy * 100.0) / 100.0 : 0.0)
                .generatedAt(now)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.parkease.analytics.dto.LotRevenueTrendResponse getLotRevenueTrend(UUID lotId, Period period) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case DAILY:
                startDate = LocalDate.now().atStartOfDay();
                break;
            case WEEKLY:
                startDate = LocalDate.now().minusDays(7).atStartOfDay();
                break;
            case MONTHLY:
                startDate = LocalDate.now().minusDays(30).atStartOfDay();
                break;
            default:
                startDate = LocalDate.now().atStartOfDay();
        }

        RevenueDto revenue = safeGetLotRevenue(lotId, startDate, now);

        return com.parkease.analytics.dto.LotRevenueTrendResponse.builder()
                .lotId(lotId)
                .period(period.name())
                .totalRevenue(revenue != null ? revenue.getTotalRevenue() : BigDecimal.ZERO)
                .currency("INR")
                .transactionCount(revenue != null ? revenue.getTransactionCount() : 0L)
                .periodStart(startDate)
                .periodEnd(now)
                .computedAt(now)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.parkease.analytics.dto.LotOccupancyTrendResponse getLotOccupancyTrend(UUID lotId, Period period) {
        enforceManagerOwnership(lotId, getCurrentUserId(), getCurrentRole());

        LotSummaryDto lot = safeGetLot(lotId);
        if (lot == null) {
            throw new ResourceNotFoundException("Lot not found: " + lotId);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case DAILY:
                startDate = LocalDate.now().atStartOfDay();
                break;
            case WEEKLY:
                startDate = LocalDate.now().minusDays(7).atStartOfDay();
                break;
            case MONTHLY:
                startDate = LocalDate.now().minusDays(30).atStartOfDay();
                break;
            default:
                startDate = LocalDate.now().atStartOfDay();
        }

        // Get hourly breakdown
        List<HourlyOccupancyResponse> hourlyBreakdown = IntStream.range(0, 24)
                .mapToObj(hour -> HourlyOccupancyResponse.builder()
                .hour(hour)
                .averageOccupancyRate(0.0)
                .build())
                .collect(Collectors.toList());

        // Calculate average occupancy rate for the period
        List<OccupancyLog> periodLogs = occupancyLogRepository
                .findByLotIdAndTimestampBetweenOrderByTimestampDesc(lotId, startDate, now);

        Double avgOccupancy = 0.0;
        Double peakOccupancy = 0.0;
        Double minOccupancy = 100.0;
        Integer avgAvailableSpots = 0;

        if (!periodLogs.isEmpty()) {
            avgOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .average()
                    .orElse(0.0);

            peakOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .max()
                    .orElse(0.0);

            minOccupancy = periodLogs.stream()
                    .mapToDouble(log -> log.getOccupancyRate() != null ? log.getOccupancyRate() : 0.0)
                    .min()
                    .orElse(0.0);

            avgAvailableSpots = (int) periodLogs.stream()
                    .mapToInt(log -> log.getAvailableSpots() != null ? log.getAvailableSpots() : 0)
                    .average()
                    .orElse(0);
        }

        return com.parkease.analytics.dto.LotOccupancyTrendResponse.builder()
                .lotId(lotId)
                .period(period.name())
                .averageOccupancyRate(Math.round(avgOccupancy * 100.0) / 100.0)
                .peakOccupancyRate(Math.round(peakOccupancy * 100.0) / 100.0)
                .minOccupancyRate(Math.round(minOccupancy * 100.0) / 100.0)
                .totalSpots(lot.getTotalSpots())
                .averageAvailableSpots(avgAvailableSpots)
                .hourlyBreakdown(hourlyBreakdown)
                .periodStart(startDate)
                .periodEnd(now)
                .computedAt(now)
                .build();
    }
}
