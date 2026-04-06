package com.parkease.analytics.service;

import com.parkease.analytics.dto.*;
import com.parkease.analytics.feign.dto.DailyRevenueDto;
import com.parkease.analytics.feign.dto.RevenueDto;
import com.parkease.analytics.rabbitmq.dto.BookingEventPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // ─── Daily Report ───
    DailyReportResponse getDailyReport(UUID lotId);
}