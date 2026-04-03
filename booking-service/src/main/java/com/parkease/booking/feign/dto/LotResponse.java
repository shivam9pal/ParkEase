package com.parkease.booking.feign.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Mirror of parkinglot-service LotResponse.
 * Used to verify lot existence and open status before booking.
 * availableSpots is managed by decrement/increment Feign calls —
 * booking-service does NOT read this value for booking logic.
 */
@Data
public class LotResponse {

    private UUID lotId;
    private String name;
    private String address;
    private String city;
    private Integer availableSpots;
    private Integer totalSpots;
    private Boolean isOpen;
    private Boolean isApproved;
}