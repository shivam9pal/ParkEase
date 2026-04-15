package com.parkease.spot.entity;

/**
 * Lifecycle state of a parking spot.
 *
 * <p>
 * Valid transitions enforced by SpotServiceImpl:
 * <pre>
 *   AVAILABLE  → RESERVED   (booking created)
 *   RESERVED   → OCCUPIED   (driver checks in)
 *   RESERVED   → AVAILABLE  (booking cancelled)
 *   OCCUPIED   → AVAILABLE  (driver checks out)
 *   AVAILABLE  → OCCUPIED   (walk-in direct check-in)
 *   AVAILABLE  → MAINTENANCE (maintenance starts)
 *   MAINTENANCE → AVAILABLE  (maintenance ends)
 * </pre> Any other transition throws a 409 CONFLICT.
 */
public enum SpotStatus {

    /**
     * Spot is free and can be booked
     */
    AVAILABLE,
    /**
     * Booking exists but driver has not yet arrived
     */
    RESERVED,
    /**
     * Driver has checked in — spot is actively in use
     */
    OCCUPIED,
    /**
     * Spot is under maintenance and unavailable
     */
    MAINTENANCE
}
