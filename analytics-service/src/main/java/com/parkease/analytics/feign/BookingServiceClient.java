package com.parkease.analytics.feign;

import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.parkease.analytics.feign.dto.BookingStatsDto;

@FeignClient(
        name = "booking-service",
        configuration = FeignConfig.class
)
public interface BookingServiceClient {

    // Used by: getPlatformSummary() to get booking statistics for a date range
    @GetMapping("/api/v1/bookings/stats")
    BookingStatsDto getBookingStats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    );
}
