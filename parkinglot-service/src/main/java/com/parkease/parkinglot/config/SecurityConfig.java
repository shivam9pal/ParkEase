package com.parkease.parkinglot.config;

import com.parkease.parkinglot.security.JwtAuthFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .httpBasic(AbstractHttpConfigurer::disable)   // no HTTP Basic Auth
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints — no JWT required (Guest + Driver browse) ──
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/lots/{lotId}",
                                "/api/v1/lots/city/**",
                                "/api/v1/lots/nearby",
                                "/api/v1/lots/search"
                        ).permitAll()

                        // ── Swagger / Actuator — public ──
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        // ── ADMIN-only endpoints ──
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/lots/all",
                                "/api/v1/lots/pending"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/lots/*/approve"
                        ).hasRole("ADMIN")

                        // ── MANAGER — create lot ──
                        .requestMatchers(HttpMethod.POST, "/api/v1/lots")
                        .hasRole("MANAGER")

                        // ── Internal service calls (booking-service) — any valid JWT ──
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/lots/*/decrement",
                                "/api/v1/lots/*/increment"
                        ).authenticated()

                        // ── All other protected endpoints ──
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",  // React CRA
                "http://localhost:5173"   // Vite
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}