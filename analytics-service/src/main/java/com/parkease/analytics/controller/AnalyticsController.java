package com.parkease.analytics.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.analytics.dto.AvgDurationResponse;
import com.parkease.analytics.dto.DailyReportResponse;
import com.parkease.analytics.dto.HourlyOccupancyResponse;
import com.parkease.analytics.dto.LotOccupancyTrendResponse;
import com.parkease.analytics.dto.LotRevenueTrendResponse;
import com.parkease.analytics.dto.LotSummaryAnalyticsResponse;
import com.parkease.analytics.dto.OccupancyRateResponse;
import com.parkease.analytics.dto.PeakHourResponse;
import com.parkease.analytics.dto.PlatformOccupancyResponse;
import com.parkease.analytics.dto.PlatformSummaryResponse;
import com.parkease.analytics.dto.SpotTypeUtilisationResponse;
import com.parkease.analytics.enums.Period;
import com.parkease.analytics.feign.dto.DailyRevenueDto;
import com.parkease.analytics.feign.dto.RevenueDto;
import com.parkease.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ─── 10.1 Real-Time Occupancy Rate ───────────────────────────────────
    @GetMapping("/occupancy/{lotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<OccupancyRateResponse> getOccupancyRate(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getOccupancyRate(lotId));
    }

    // ─── 10.2 Hourly Occupancy Breakdown ─────────────────────────────────
    @GetMapping("/occupancy/{lotId}/hourly")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<HourlyOccupancyResponse>> getOccupancyByHour(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getOccupancyByHour(lotId));
    }

    // ─── 10.3 Peak Hours Analysis ─────────────────────────────────────────
    @GetMapping("/occupancy/{lotId}/peak")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<PeakHourResponse>> getPeakHours(
            @PathVariable UUID lotId,
            @RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(analyticsService.getPeakHours(lotId, topN));
    }

    // ─── 10.4 Lot Revenue ────────────────────────────────────────────────
    @GetMapping("/revenue/{lotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RevenueDto> getLotRevenue(
            @PathVariable UUID lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getLotRevenue(lotId, from, to));
    }

    // ─── 10.5 Daily Revenue Breakdown ────────────────────────────────────
    @GetMapping("/revenue/{lotId}/daily")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<DailyRevenueDto>> getLotDailyRevenue(
            @PathVariable UUID lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getLotDailyRevenue(lotId, from, to));
    }

    // ─── 10.6 Spot Type Utilisation ──────────────────────────────────────
    @GetMapping("/spot-types/{lotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<SpotTypeUtilisationResponse>> getSpotTypeUtilisation(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getSpotTypeUtilisation(lotId));
    }

    // ─── 10.7 Average Parking Duration ───────────────────────────────────
    @GetMapping("/avg-duration/{lotId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<AvgDurationResponse> getAvgDuration(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getAvgDuration(lotId));
    }

    // ─── 10.8 Platform Summary (ADMIN only) ──────────────────────────────
    @GetMapping("/platform/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformSummaryResponse> getPlatformSummary() {
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }

    // ─── 10.8.1 Platform Occupancy (ADMIN only) ─────────────────────
    @GetMapping("/platform/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformOccupancyResponse> getPlatformOccupancy(
            @RequestParam(defaultValue = "WEEKLY") Period period) {
        return ResponseEntity.ok(analyticsService.getPlatformOccupancy(period));
    }

    // ─── 10.9 Daily Report ───────────────────────────────────────────────
    @GetMapping("/report/{lotId}/daily")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getDailyReport(lotId));
    }

    // ─── 10.10 Lot Summary Analytics ─────────────────────────────────────
    @GetMapping("/lots/{lotId}/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LotSummaryAnalyticsResponse> getLotAnalyticsSummary(
            @PathVariable UUID lotId) {
        return ResponseEntity.ok(analyticsService.getLotAnalyticsSummary(lotId));
    }

    // ─── 10.11 Lot Revenue Trend ────────────────────────────────────────
    @GetMapping("/lots/{lotId}/revenue")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LotRevenueTrendResponse> getLotRevenueTrend(
            @PathVariable UUID lotId,
            @RequestParam(defaultValue = "WEEKLY") Period period) {
        return ResponseEntity.ok(analyticsService.getLotRevenueTrend(lotId, period));
    }

    // ─── 10.12 Lot Occupancy Trend ──────────────────────────────────────
    @GetMapping("/lots/{lotId}/occupancy")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LotOccupancyTrendResponse> getLotOccupancyTrend(
            @PathVariable UUID lotId,
            @RequestParam(defaultValue = "WEEKLY") Period period) {
        return ResponseEntity.ok(analyticsService.getLotOccupancyTrend(lotId, period));
    }
}
