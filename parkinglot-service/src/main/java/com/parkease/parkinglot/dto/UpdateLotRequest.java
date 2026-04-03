package com.parkease.parkinglot.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateLotRequest {
    private String name;       // Optional — null fields are ignored
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String imageUrl;
}