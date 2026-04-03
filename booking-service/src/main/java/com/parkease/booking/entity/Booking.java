package com.parkease.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID bookingId;

    // Cross-service reference — NO JPA join to auth-service
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Cross-service reference — NO JPA join to parkinglot-service
    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    // Cross-service reference — NO JPA join to spot-service
    @Column(name = "spot_id", nullable = false)
    private UUID spotId;

    // Cross-service reference — NO JPA join to vehicle-service
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Denormalized — stored for quick display without cross-service call
    @Column(name = "vehicle_plate", nullable = false)
    private String vehiclePlate;

    // Denormalized — stored for quick display without cross-service call
    @Column(name = "vehicle_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(name = "booking_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingType bookingType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Nullable — set at actual check-in, not at booking creation (PRE_BOOKING)
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    // Nullable — set at checkout
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    // Nullable until checkout — NEVER return 0.0 for un-checked-out bookings
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Snapshotted from spot at booking time — NEVER re-fetched during checkout
    // BigDecimal mandatory — float/double forbidden for monetary values
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // CRITICAL — optimistic locking prevents two drivers booking the same spot
    // simultaneously. @Transactional alone does NOT prevent race conditions.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}