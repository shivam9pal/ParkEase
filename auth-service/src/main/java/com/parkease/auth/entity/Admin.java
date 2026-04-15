package com.parkease.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID adminId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    // Always BCrypt — no OAuth2 for admins
    @Column(nullable = false)
    private String passwordHash;

    // Persist explicit role in admin table instead of inferring only from JWT generation
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ADMIN'")
    @Builder.Default
    private User.Role role = User.Role.ADMIN;

    @Column(nullable = false)
    private boolean isActive = true;

    // Only the YML-seeded Super Admin has this as true
    // All admins created via /admin/create always get false
    @Column(nullable = false)
    private boolean isSuperAdmin = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void applyDefaults() {
        if (role == null) {
            role = User.Role.ADMIN;
        }
    }
}
