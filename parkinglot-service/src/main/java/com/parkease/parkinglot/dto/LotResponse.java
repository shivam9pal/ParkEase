package com.parkease.parkinglot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class LotResponse {
    private UUID lotId;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private Integer totalSpots;
    private Integer availableSpots;
    private UUID managerId;
    private Boolean isOpen;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String imageUrl;
    private Boolean isApproved;
    private LocalDateTime createdAt;
}