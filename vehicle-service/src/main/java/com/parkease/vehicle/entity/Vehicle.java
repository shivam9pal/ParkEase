package com.parkease.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "vehicles",
        uniqueConstraints = {
                // Enforce unique license plate per owner at DB level
                @UniqueConstraint(
                        name = "uk_owner_license_plate",
                        columnNames = {"owner_id", "license_plate"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "vehicle_id", updatable = false, nullable = false)
    private UUID vehicleId;

    /**
     * References the userId from auth-service.
     * No JPA join across services — UUID by reference only.
     */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Column(name = "make", nullable = false)
    private String make;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "color")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    /**
     * EV flag — booking-service uses this to match EV vehicles
     * to EV-charging spots only.
     */
    @Column(name = "is_ev", nullable = false)
    private Boolean isEV = false;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    /**
     * Soft-delete flag. deleteVehicle() sets this to false.
     * Records are never hard-deleted.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
        if (this.isEV == null)    this.isEV    = false;
        if (this.isActive == null) this.isActive = true;
    }
}