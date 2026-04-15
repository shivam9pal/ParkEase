package com.parkease.analytics.feign;

import com.parkease.analytics.feign.dto.DailyRevenueDto;
import com.parkease.analytics.feign.dto.RevenueDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "payment-service",
//        url = "${services.payment.url}",
        configuration = FeignConfig.class
)
public interface PaymentServiceClient {

    // Used by: GET /api/v1/analytics/revenue/{lotId}
    @GetMapping("/api/v1/payments/revenue/lot/{lotId}")
    RevenueDto getLotRevenue(
            @PathVariable("lotId") UUID lotId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    );

    // Used by: GET /api/v1/analytics/revenue/{lotId}/daily
    @GetMapping("/api/v1/payments/revenue/lot/{lotId}/daily")
    List<DailyRevenueDto> getLotDailyRevenue(
            @PathVariable("lotId") UUID lotId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    );

    // Used by: GET /api/v1/analytics/platform/summary (ADMIN only)
    @GetMapping("/api/v1/payments/revenue/platform")
    RevenueDto getPlatformRevenue(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    );
}