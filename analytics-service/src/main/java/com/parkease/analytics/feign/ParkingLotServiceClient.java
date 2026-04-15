package com.parkease.analytics.feign;

import com.parkease.analytics.feign.dto.LotSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "parkinglot-service",
//        url = "${services.parkinglot.url}",
        configuration = FeignConfig.class
)
public interface ParkingLotServiceClient {

    // Used by: BookingEventConsumer (lot snapshot at event time) + OccupancySnapshotScheduler
    @GetMapping("/api/v1/lots/{lotId}")
    LotSummaryDto getLotById(@PathVariable("lotId") UUID lotId);

    // Used by: logScheduledSnapshots() + getPlatformSummary()
    // Returns ALL lots (approved + pending) — requires ADMIN system JWT
    @GetMapping("/api/v1/lots/all")
    List<LotSummaryDto> getAllLots();
}