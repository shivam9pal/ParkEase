package com.parkease.auth.service;

import com.parkease.auth.config.AdminConfig;
import com.parkease.auth.entity.Admin;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final AdminConfig adminConfig;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        backfillAdminRoleIfMissing();

        // If a Super Admin already exists, skip seeding entirely
        if (adminRepository.existsByIsSuperAdminTrue()) {
            log.info("Super Admin already exists. Skipping seed.");
            return;
        }

        log.info("Seeding Super Admin from application.yml...");

        Admin superAdmin = Admin.builder()
                .fullName(adminConfig.getSeedName())
                .email(adminConfig.getSeedEmail())
                .passwordHash(passwordEncoder.encode(adminConfig.getSeedPassword()))
                .role(User.Role.ADMIN)
                .isActive(true)
                .isSuperAdmin(true)
                .createdAt(LocalDateTime.now())
                .build();

        adminRepository.save(superAdmin);

        log.info("Super Admin created successfully: {}", adminConfig.getSeedEmail());
    }

    private void backfillAdminRoleIfMissing() {
        List<Admin> admins = adminRepository.findAll();
        boolean changed = false;

        for (Admin admin : admins) {
            if (admin.getRole() == null) {
                admin.setRole(User.Role.ADMIN);
                changed = true;
            }
        }

        if (changed) {
            adminRepository.saveAll(admins);
            log.info("Backfilled missing admin roles to ADMIN for existing records.");
        }
    }
}
