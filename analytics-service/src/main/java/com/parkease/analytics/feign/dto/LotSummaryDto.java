package com.parkease.analytics.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotSummaryDto {
    private UUID lotId;
    private String name;
    private String city;
    private Integer totalSpots;
    private Integer availableSpots;
    private Boolean isOpen;
    private Boolean isApproved;
    private UUID managerId;
}