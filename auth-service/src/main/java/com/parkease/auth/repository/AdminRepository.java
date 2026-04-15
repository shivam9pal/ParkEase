package com.parkease.auth.repository;

import com.parkease.auth.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {

    // Used for admin login lookup
    Optional<Admin> findByEmail(String email);

    // Used for Super Admin verification in protected endpoints
    Optional<Admin> findByAdminId(UUID adminId);

    // Used to prevent duplicate admin emails on create
    boolean existsByEmail(String email);

    // Used by AdminSeeder — if true, skip seeding
    boolean existsByIsSuperAdminTrue();

    // Used by getAllAdmins() — newest first
    List<Admin> findAllByOrderByCreatedAtDesc();
}