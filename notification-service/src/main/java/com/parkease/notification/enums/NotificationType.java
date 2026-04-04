package com.parkease.notification.enums;

public enum NotificationType {
    BOOKING_CREATED,    // New booking confirmed (PRE_BOOKING or WALK_IN)
    CHECKIN,            // Driver checked in to spot
    CHECKOUT,           // Driver checked out, fare calculated
    BOOKING_CANCELLED,  // Booking cancelled (by driver, manager, admin, or auto-expiry)
    BOOKING_EXTENDED,   // Booking duration extended
    PAYMENT_COMPLETED,  // Payment processed successfully
    PAYMENT_REFUNDED,   // Refund issued
    PROMO               // Admin broadcast / promotional message
}