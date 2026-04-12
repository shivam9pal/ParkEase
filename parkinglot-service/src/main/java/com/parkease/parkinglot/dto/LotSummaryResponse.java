package com.parkease.parkinglot.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for frontend lot listing/management pages Contains only essential fields
 * for lot summary display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotSummaryResponse {

    private UUID lotId;
    private String name;
    private String address;
    private UUID managerId;
    private Integer totalSpots;
    private Integer availableSpots;
    private Boolean isApproved;
    private LocalDateTime createdAt;
}
