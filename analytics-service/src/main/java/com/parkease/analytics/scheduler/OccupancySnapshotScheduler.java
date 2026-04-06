package com.parkease.analytics.scheduler;

import com.parkease.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OccupancySnapshotScheduler {

    private final AnalyticsService analyticsService;

    // Runs every 15 minutes — builds the time-series used for hourly/peak analysis
    // fixedDelay = next run starts AFTER current run finishes (safer than fixedRate)
    @Scheduled(fixedDelay = 900_000)
    public void snapshotAllLots() {
        log.info("Starting scheduled occupancy snapshot for all approved lots...");
        try {
            analyticsService.logScheduledSnapshots();
        } catch (Exception e) {
            // ⚠️ NEVER let exceptions propagate out of @Scheduled
            // An uncaught exception here would stop ALL future scheduled runs
            log.error("Occupancy snapshot run failed: {}", e.getMessage(), e);
        }
    }
}