package com.parkease.analytics.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotSummaryDto {
    private UUID spotId;
    private UUID lotId;
    private String spotType;     // COMPACT / STANDARD / LARGE / MOTORBIKE / EV
    private String vehicleType;  // TWO_WHEELER / FOUR_WHEELER / HEAVY
    private String status;
    private Boolean isEVCharging;
    private Boolean isHandicapped;
}