package com.parkease.notification.config;

import com.parkease.notification.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Driver / Manager — own notification inbox
                        .requestMatchers(HttpMethod.GET,  "/api/v1/notifications/my").hasAnyRole("DRIVER", "MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/notifications/my/unread").hasAnyRole("DRIVER", "MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/notifications/my/unread/count").hasAnyRole("DRIVER", "MANAGER")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/notifications/*/read").hasAnyRole("DRIVER", "MANAGER")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/notifications/my/read-all").hasAnyRole("DRIVER", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/*").hasAnyRole("DRIVER", "ADMIN")

                        // Admin only
                        .requestMatchers(HttpMethod.GET,  "/api/v1/notifications/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/broadcast").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}