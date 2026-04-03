package com.parkease.booking.entity;

public enum BookingStatus {
    RESERVED,       // Booking created, awaiting check-in (PRE_BOOKING only)
    ACTIVE,         // Driver checked in — spot is OCCUPIED
    COMPLETED,      // Driver checked out — fare calculated, payment triggered
    CANCELLED       // Cancelled by driver, manager, or system (expired)
}