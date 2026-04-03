package com.parkease.spot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a single physical parking space within a lot.
 *
 * <p>lotId is a cross-service reference — NO JPA join to parkinglot-service.
 * pricePerHour is BigDecimal — never double/float (monetary precision required).
 */
@Entity
@Table(
        name = "parking_spot",
        uniqueConstraints = {
                // Same spotNumber cannot exist twice within the same lot
                @UniqueConstraint(
                        name = "uk_lot_spot_number",
                        columnNames = {"lot_id", "spot_number"}
                )
        },
        indexes = {
                @Index(name = "idx_spot_lot_id",      columnList = "lot_id"),
                @Index(name = "idx_spot_lot_status",  columnList = "lot_id, status"),
                @Index(name = "idx_spot_lot_type",    columnList = "lot_id, spot_type"),
                @Index(name = "idx_spot_vehicle_type",columnList = "lot_id, vehicle_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    // ─────────────────────────────── Primary Key ──────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "spot_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID spotId;

    // ─────────────────────────── Cross-Service Reference ─────────────────────

    /**
     * References the parking lot in parkinglot-service.
     * NO @ManyToOne or @JoinColumn — cross-service boundary, UUID only.
     */
    @Column(name = "lot_id", nullable = false, columnDefinition = "uuid")
    private UUID lotId;

    // ─────────────────────────────── Core Fields ──────────────────────────────

    /**
     * Human-readable spot identifier unique per lot.
     * Examples: "A-01", "B-12", "G-Floor-01"
     */
    @Column(name = "spot_number", nullable = false, length = 50)
    private String spotNumber;

    /**
     * Floor level: 0 = Ground Floor, 1 = First Floor, -1 = Basement 1, etc.
     */
    @Column(name = "floor", nullable = false)
    private Integer floor;

    // ─────────────────────────────── ENUM Fields ──────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 20)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    /**
     * Current lifecycle state of this spot.
     * Transitions strictly enforced in SpotServiceImpl.
     * Default: AVAILABLE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SpotStatus status = SpotStatus.AVAILABLE;

    // ─────────────────────────────── Feature Flags ────────────────────────────

    /**
     * True if this spot is accessible for disabled drivers.
     * Default: false
     */
    @Column(name = "is_handicapped", nullable = false)
    @Builder.Default
    private Boolean isHandicapped = false;

    /**
     * True if this spot has an EV charging station.
     * Automatically set to true when spotType = EV.
     * Default: false
     */
    @Column(name = "is_ev_charging", nullable = false)
    @Builder.Default
    private Boolean isEVCharging = false;

    // ──────────────────────────── Monetary Field ──────────────────────────────

    /**
     * Hourly rate for this specific spot.
     * Used by booking-service for fare calculation.
     * BigDecimal — NEVER double or float (monetary precision).
     * precision=10, scale=2 supports values up to 99,999,999.99
     */
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    // ─────────────────────────────── Audit Field ──────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ──────────────────────────────── Lifecycle ───────────────────────────────

    /**
     * Auto-sets createdAt before first persist.
     * Also enforces the EV → isEVCharging=true business rule at entity level.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // Business rule: EV spot type must always have charging enabled
        if (SpotType.EV.equals(this.spotType)) {
            this.isEVCharging = true;
        }

        // Ensure status defaults if somehow null
        if (this.status == null) {
            this.status = SpotStatus.AVAILABLE;
        }
    }
}