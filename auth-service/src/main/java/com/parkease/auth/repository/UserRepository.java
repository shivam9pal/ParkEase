package com.parkease.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkease.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    List<User> findAllByRole(User.Role role);

    List<User> findAllByRoleAndIsActive(User.Role role, Boolean isActive);

    List<User> findAllByIsActive(Boolean isActive);

    Optional<User> findByVehiclePlate(String vehiclePlate);

    Optional<User> findByPhone(String phone);

    void deleteByUserId(UUID userId);
}
