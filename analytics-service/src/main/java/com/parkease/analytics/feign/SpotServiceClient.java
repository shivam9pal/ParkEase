package com.parkease.analytics.feign;

import com.parkease.analytics.feign.dto.SpotSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "spot-service",
        url = "${services.spot.url}",
        configuration = FeignConfig.class
)
public interface SpotServiceClient {

    // Used ONLY by BookingEventConsumer on BOOKING_CREATED events
    // to capture spotType for spot-type utilisation analytics
    @GetMapping("/api/v1/spots/{spotId}")
    SpotSummaryDto getSpotById(@PathVariable("spotId") UUID spotId);
}