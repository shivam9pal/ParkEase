package com.parkease.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotTypeUtilisationResponse {
    private String spotType;       // COMPACT / STANDARD / LARGE / MOTORBIKE / EV
    private Long bookingCount;
    private Double percentage;
}