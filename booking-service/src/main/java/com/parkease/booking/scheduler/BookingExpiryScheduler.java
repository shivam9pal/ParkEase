package com.parkease.booking.scheduler;

import com.parkease.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job — auto-cancels expired PRE_BOOKING reservations.
 *
 * A PRE_BOOKING is considered expired when:
 *   booking.status == RESERVED
 *   AND booking.bookingType == PRE_BOOKING
 *   AND booking.startTime < (now - gracePeriodMinutes)
 *
 * Default grace period: 30 minutes (configurable via application.yaml).
 * Run frequency: every 5 minutes (fixedDelay = 300_000 ms).
 *
 * Uses fixedDelay (not fixedRate) — next run begins 5 minutes AFTER
 * the previous run completes, preventing overlap if a batch runs long.
 *
 * NOTE: This scheduler has NO JWT context — it runs as a system operation.
 * FeignConfig's RequestInterceptor null-checks RequestContextHolder,
 * so it safely skips JWT forwarding for scheduler-triggered Feign calls.
 * Ensure spot-service and parkinglot-service internal endpoints
 * (release, increment) are accessible from internal network in production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    @Value("${booking.expiry.grace-period-minutes:30}")
    private int gracePeriodMinutes;

    /**
     * Fires every 5 minutes.
     * Delegates all logic to BookingServiceImpl.autoExpireBookings()
     * which handles: find → releaseSpot → incrementLot → cancel → publish event.
     */
    @Scheduled(fixedDelay = 300_000)
    public void autoExpireBookings() {
        log.info("[Scheduler] Running booking expiry check. Grace period: {} minutes.",
                gracePeriodMinutes);
        try {
            bookingService.autoExpireBookings();
        } catch (Exception e) {
            // Scheduler must never crash the application — catch all exceptions
            log.error("[Scheduler] Expiry job encountered an unexpected error: {}",
                    e.getMessage(), e);
        }
    }
}