package com.parkease.auth.config;

import com.parkease.auth.security.JwtAuthFilter;
import com.parkease.auth.security.UserDetailsServiceImpl;
import com.parkease.auth.security.oauth2.OAuth2SuccessHandler;
import com.parkease.auth.security.oauth2.OAuth2UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // ── Existing public endpoints (UNCHANGED) ─────────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/oauth2/**",
            "/login/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // ── New OTP public endpoints ───────────────────────────────────────────────
    private static final String[] OTP_PUBLIC_ENDPOINTS = {
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // ── Existing public endpoints (UNCHANGED) ─────────────
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // ── New OTP endpoints (public) ─────────────────────────
                        .requestMatchers(OTP_PUBLIC_ENDPOINTS).permitAll()

                        // ── New Admin public endpoint ──────────────────────────
                        .requestMatchers("/api/v1/auth/admin/login").permitAll()

                        // ── New Admin protected endpoints (ADMIN role only) ─────
                        // isSuperAdmin check is handled inside the service layer
                        .requestMatchers("/api/v1/auth/admin/create").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/admin/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/auth/admin/**").hasRole("ADMIN")

                        // ── Everything else needs authentication ───────────────
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        // Keep IF_REQUIRED — OAuth2 authorization code flow needs
                        // a brief session. JWT routes remain stateless.
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
