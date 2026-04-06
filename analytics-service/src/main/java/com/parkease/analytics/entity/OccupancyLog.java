package com.parkease.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "occupancy_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", nullable = false, updatable = false)
    private UUID logId;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "spot_id")
    private UUID spotId;                 // null for SCHEDULED snapshots

    @Column(name = "spot_type", length = 20)
    private String spotType;             // COMPACT/STANDARD/LARGE/MOTORBIKE/EV

    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;          // TWO_WHEELER/FOUR_WHEELER/HEAVY

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;            // BOOKING_CREATED/CHECKIN/CHECKOUT/CANCELLED/SCHEDULED

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;     // Set by @PrePersist — immutable

    @Column(name = "occupancy_rate")
    private Double occupancyRate;        // (totalSpots - availableSpots) / totalSpots * 100

    @Column(name = "available_spots")
    private Integer availableSpots;

    @Column(name = "total_spots")
    private Integer totalSpots;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;   // null except CHECKIN/CHECKOUT events

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;  // null except CHECKOUT events

    @Column(name = "duration_minutes")
    private Long durationMinutes;        // Only populated for CHECKOUT events

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
