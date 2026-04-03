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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

                // ── CORS — allow frontend origins ─────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Session: STATELESS — no HttpSession created or used ───────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // no HTTP Basic Auth
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // ── Authorization Rules ───────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints — no JWT required
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
                        // Ownership check enforced in service layer
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/checkout")
                        .authenticated()

                        // ── PUT: Cancel — any authenticated user ─────────────────────
                        // Role-based access enforced in cancelBooking() service method
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel")
                        .authenticated()

                        // ── GET: Single booking by ID — any authenticated user ────────
                        // Ownership check enforced in controller
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*")
                        .authenticated()

                        // ── GET: Fare estimate — any authenticated user ───────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*/fare")
                        .authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // ── Inject JWT filter before Spring's username/password filter ────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── CORS Configuration ───────────────────────────────────────────────────

    /**
     * Allows frontend origins to call booking-service APIs.
     * React CRA (3000) and Vite (5173) both permitted.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // React CRA
                "http://localhost:5173"    // Vite / React
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}