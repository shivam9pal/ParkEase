package com.parkease.vehicle.config;

import com.parkease.vehicle.security.JwtAuthFilter;
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
 * Security configuration for vehicle-service.
 *
 * ── Rules ──
 *  Public  : Swagger docs endpoints (no auth needed)
 *  Protected: ALL /api/v1/vehicles/** require valid JWT
 *
 * ── No login/register here ──
 *  Auth is handled exclusively by auth-service (port 8081).
 *  This service only VALIDATES tokens — it never issues them.
 *
 * ── No UserDetailsService ──
 *  vehicle-service has no user table. Authentication is purely
 *  token-based via JwtAuthFilter and the JWT claims.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS — no JWT required
    // ─────────────────────────────────────────────────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Disable CSRF — REST API is stateless ──
                .csrf(AbstractHttpConfigurer::disable)

                // ❌ REMOVED: .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Stateless session — no HttpSession created or used ──
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // ── Endpoint access rules ──
                .authorizeHttpRequests(auth -> auth

                        // Swagger and actuator — public
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Admin-only endpoint
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/all").hasRole("ADMIN")

                        // All other vehicle endpoints — any authenticated user
                        // Fine-grained owner checks are done inside VehicleResource
                        .requestMatchers("/api/v1/vehicles/**").authenticated()

                        // Deny everything else
                        .anyRequest().denyAll()
                )

                // ── Insert JWT filter before Spring's username/password filter ──
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ❌ REMOVED: entire corsConfigurationSource() bean
}