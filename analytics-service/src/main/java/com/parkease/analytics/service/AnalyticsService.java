package com.parkease.analytics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.parkease.analytics.dto.AvgDurationResponse;
import com.parkease.analytics.dto.DailyReportResponse;
import com.parkease.analytics.dto.HourlyOccupancyResponse;
import com.parkease.analytics.dto.OccupancyRateResponse;
import com.parkease.analytics.dto.PeakHourResponse;
import com.parkease.analytics.dto.PlatformOccupancyResponse;
import com.parkease.analytics.dto.PlatformSummaryResponse;
import com.parkease.analytics.dto.SpotTypeUtilisationResponse;
import com.parkease.analytics.enums.Period;
import com.parkease.analytics.feign.dto.DailyRevenueDto;
import com.parkease.analytics.feign.dto.RevenueDto;
import com.parkease.analytics.rabbitmq.dto.BookingEventPayload;

public interface AnalyticsService {

    // ─── Event + Scheduler Writers ───
    void processBookingEvent(BookingEventPayload payload, String routingKey);

    void logScheduledSnapshots();

    // ─── Occupancy Metrics ───
    OccupancyRateResponse getOccupancyRate(UUID lotId);

    List<HourlyOccupancyResponse> getOccupancyByHour(UUID lotId);

    List<PeakHourResponse> getPeakHours(UUID lotId, int topN);

    // ─── Revenue Metrics (delegated to payment-service) ───
    RevenueDto getLotRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    List<DailyRevenueDto> getLotDailyRevenue(UUID lotId, LocalDateTime from, LocalDateTime to);

    // ─── Utilisation & Duration ───
    List<SpotTypeUtilisationResponse> getSpotTypeUtilisation(UUID lotId);

    AvgDurationResponse getAvgDuration(UUID lotId);

    // ─── Platform Summary (ADMIN only) ───
    PlatformSummaryResponse getPlatformSummary();

    PlatformOccupancyResponse getPlatformOccupancy(Period period);

    // ─── Daily Report ───
    DailyReportResponse getDailyReport(UUID lotId);

    // ─── Lot Analytics (New) ───
    com.parkease.analytics.dto.LotSummaryAnalyticsResponse getLotAnalyticsSummary(UUID lotId);

    com.parkease.analytics.dto.LotRevenueTrendResponse getLotRevenueTrend(UUID lotId, Period period);

    com.parkease.analytics.dto.LotOccupancyTrendResponse getLotOccupancyTrend(UUID lotId, Period period);
}
