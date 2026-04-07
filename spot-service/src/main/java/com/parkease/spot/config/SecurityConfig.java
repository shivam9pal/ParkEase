package com.parkease.spot.config;

import com.parkease.spot.security.JwtAuthFilter;
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
 * Spring Security configuration for spot-service.
 *
 * <p>Session policy: STATELESS — pure JWT, no HttpSession created.
 *
 * <p>Access control summary:
 * <pre>
 *   PUBLIC  (no JWT):
 *     GET  /api/v1/spots/{spotId}          — spot details (booking-service + guests)
 *     GET  /api/v1/spots/lot/**             — all lot-level browse endpoints
 *     GET  /v3/api-docs/**                  — OpenAPI spec
 *     GET  /swagger-ui/**                   — Swagger UI assets
 *     GET  /swagger-ui.html                 — Swagger UI entry
 *     GET  /actuator/health                 — health probe
 *
 *   MANAGER only (JWT + ROLE_MANAGER):
 *     POST /api/v1/spots                    — add single spot
 *     POST /api/v1/spots/bulk               — bulk add spots
 *     PUT  /api/v1/spots/{id}               — update spot metadata
 *
 *   MANAGER or ADMIN (JWT + ROLE_MANAGER | ROLE_ADMIN):
 *     DELETE /api/v1/spots/{id}             — delete spot
 *
 *   Any valid JWT (booking-service internal calls):
 *     PUT  /api/v1/spots/{id}/reserve       — AVAILABLE → RESERVED
 *     PUT  /api/v1/spots/{id}/occupy        — RESERVED/AVAILABLE → OCCUPIED
 *     PUT  /api/v1/spots/{id}/release       — RESERVED/OCCUPIED → AVAILABLE
 * </pre>
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
                // ── CSRF disabled — stateless REST API, JWT-based ─────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ❌ REMOVED: .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Authorization rules ───────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // ── Swagger / OpenAPI — fully public ──────────────────────────
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ── Actuator health probe — public ────────────────────────────
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ── Public GET: single spot details ───────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/spots/{spotId}"
                        ).permitAll()

                        // ── Public GET: all lot-level browse endpoints ─────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/spots/lot/**"
                        ).permitAll()

                        // ── Internal: booking-service status transitions ───────────────
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/spots/*/reserve",
                                "/api/v1/spots/*/occupy",
                                "/api/v1/spots/*/release"
                        ).authenticated()

                        // ── Manager-only: create spots ────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/spots",
                                "/api/v1/spots/bulk"
                        ).hasRole("MANAGER")

                        // ── Manager-only: update spot metadata ────────────────────────
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/spots/*"
                        ).hasRole("MANAGER")

                        // ── Manager or Admin: delete spot ─────────────────────────────
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/spots/*"
                        ).hasAnyRole("MANAGER", "ADMIN")

                        // ── Catch-all ─────────────────────────────────────────────────
                        .anyRequest().authenticated()
                )

                // ── Stateless session — no HttpSession, no cookies ────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ── Disable default form login and HTTP Basic ─────────────────────
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // ── Register JWT filter before Spring's auth filter ───────────────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ❌ REMOVED: entire corsConfigurationSource() bean
}