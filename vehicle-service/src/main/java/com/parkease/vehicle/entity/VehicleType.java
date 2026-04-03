package com.parkease.vehicle.entity;

/**
 * Vehicle classification used for spot-type matching in booking-service.
 *
 *  TWO_WHEELER  → 2W spots  (motorcycles, scooters)
 *  FOUR_WHEELER → 4W spots  (cars, SUVs)
 *  HEAVY        → Heavy spots (trucks, buses)
 */
public enum VehicleType {
    TWO_WHEELER,
    FOUR_WHEELER,
    HEAVY
}