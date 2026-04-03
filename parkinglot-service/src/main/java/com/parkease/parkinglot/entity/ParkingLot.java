package com.parkease.parkinglot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "parking_lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lot_id", updatable = false, nullable = false)
    private UUID lotId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer totalSpots;

    @Column(nullable = false)
    private Integer availableSpots;

    // Cross-service reference — no JPA join; just UUID
    @Column(nullable = false)
    private UUID managerId;

    @Column(nullable = false)
    private Boolean isOpen;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column
    private String imageUrl;

    // Admin approval flag — default FALSE — lot hidden until approved
    @Column(nullable = false)
    private Boolean isApproved;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optimistic locking — prevents double-booking race conditions
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isOpen == null)    this.isOpen = true;
        if (this.isApproved == null) this.isApproved = false;
    }
}