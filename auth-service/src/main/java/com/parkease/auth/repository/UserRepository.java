package com.parkease.auth.repository;

import com.parkease.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    List<User> findAllByRole(User.Role role);

    Optional<User> findByVehiclePlate(String vehiclePlate);

    Optional<User> findByPhone(String phone);

    void deleteByUserId(UUID userId);
}
