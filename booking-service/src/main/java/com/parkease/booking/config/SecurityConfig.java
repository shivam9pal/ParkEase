package com.parkease.booking.config;

import com.parkease.booking.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// ❌ REMOVED: CorsConfiguration, CorsConfigurationSource,
//             UrlBasedCorsConfigurationSource, List

/**
 * Spring Security configuration for booking-service.
 *
 * Policy: STATELESS — no HttpSession, no cookies, pure JWT.
 *
 * Role hierarchy:
 *   ROLE_DRIVER   → own bookings only (ownership enforced in service layer)
 *   ROLE_MANAGER  → lot-scoped operations
 *   ROLE_ADMIN    → unrestricted platform-wide access
 *
 * Note: hasRole("X") in Spring Security automatically prepends "ROLE_"
 * so hasRole("DRIVER") matches the "ROLE_DRIVER" authority set by JwtAuthFilter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ── Disable CSRF — stateless REST API, no browser session ─────────
                .csrf(AbstractHttpConfigurer::disable)

                // ❌ REMOVED: .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Session: STATELESS — no HttpSession created or used ───────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // ── Authorization Rules ───────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        // ── POST: Create booking — DRIVER only ────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
                        .hasRole("DRIVER")

                        // ── GET: Driver's own bookings ────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my")
                        .hasRole("DRIVER")

                        // ── GET: Driver's booking history ─────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/history")
                        .hasRole("DRIVER")

                        // ── GET: All bookings platform-wide — ADMIN only ──────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/all")
                        .hasRole("ADMIN")

                        // ── GET: All bookings for a lot — MANAGER or ADMIN ───────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/lot/**")
                        .hasAnyRole("MANAGER", "ADMIN")

                        // ── PUT: Check-in — DRIVER only ───────────────────────────────
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/checkin")
                        .hasRole("DRIVER")

                        // ── PUT: Extend — DRIVER only ─────────────────────────────────
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/extend")
                        .hasRole("DRIVER")

                        // ── PUT: Checkout — DRIVER, MANAGER, or ADMIN ────────────────
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/checkout")
                        .authenticated()

                        // ── PUT: Cancel — any authenticated user ─────────────────────
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel")
                        .authenticated()

                        // ── GET: Single booking by ID — any authenticated user ────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*")
                        .authenticated()

                        // ── GET: Fare estimate — any authenticated user ───────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*/fare")
                        .authenticated()

                        .anyRequest().authenticated()
                )

                // ── Inject JWT filter before Spring's username/password filter ────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ❌ REMOVED: entire corsConfigurationSource() bean
}