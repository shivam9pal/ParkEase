package com.parkease.spot.entity;

/**
 * Defines the physical category of a parking spot.
 * Used for spot filtering and booking compatibility checks.
 */
public enum SpotType {

    /** Small car-sized slot */
    COMPACT,

    /** Standard car-sized slot */
    STANDARD,

    /** SUV / large vehicle slot */
    LARGE,

    /** Dedicated two-wheeler slot */
    MOTORBIKE,

    /** Electric vehicle slot — always has EV charging */
    EV
}