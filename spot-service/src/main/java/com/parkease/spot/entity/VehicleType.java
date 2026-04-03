package com.parkease.spot.entity;

/**
 * Vehicle category used for spot-to-vehicle compatibility matching.
 * Must mirror the VehicleType enum in vehicle-service exactly.
 */
public enum VehicleType {

    /** Motorcycles, scooters, mopeds */
    TWO_WHEELER,

    /** Cars, SUVs, sedans */
    FOUR_WHEELER,

    /** Trucks, buses, heavy commercial vehicles */
    HEAVY
}