package com.parkease.auth.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.parkease.auth.entity.Admin;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.AdminRepository;
import com.parkease.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                throw new RuntimeException("User password is missing for: " + email);
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getIsActive(),
                    true, true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        }

        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();

            if (admin.getPasswordHash() == null || admin.getPasswordHash().isEmpty()) {
                throw new RuntimeException("Admin password is missing for: " + email);
            }

            return new org.springframework.security.core.userdetails.User(
                    admin.getEmail(),
                    admin.getPasswordHash(),
                    admin.isActive(),
                    true, true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
            );
        }

        throw new UsernameNotFoundException("User not found: " + email);
    }
}