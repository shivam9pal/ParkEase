package com.parkease.vehicle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intercepts every request exactly once, validates the JWT Bearer token,
 * and populates the Spring SecurityContext with email + role + userId.
 *
 * No database lookup here — vehicle-service is fully stateless.
 * userId and role are stored in authentication.getDetails() as a Map,
 * so VehicleResource can extract them without re-parsing the token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ── 1. Skip if no Bearer token present ──
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7); // strip "Bearer "

        try {
            // ── 2. Validate token signature + expiry ──
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token — request to {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // ── 3. Extract claims ──
            String email  = jwtUtil.extractEmail(token);
            String role   = jwtUtil.extractRole(token);  // e.g. "DRIVER"
            var    userId = jwtUtil.extractUserId(token); // UUID

            // ── 4. Only set auth if not already authenticated ──
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Spring Security authority must be prefixed with ROLE_
                var authority = new SimpleGrantedAuthority("ROLE_" + role);

                var authToken = new UsernamePasswordAuthenticationToken(
                        email,        // principal (email string)
                        null,         // credentials (not needed — token already validated)
                        List.of(authority)
                );

                // ── 5. Attach userId + role to details so controllers can read them ──
                Map<String, Object> details = new HashMap<>();
                details.put("userId", userId);
                details.put("role", role);
                authToken.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated userId={} role={} for URI={}",
                        userId, role, request.getRequestURI());
            }

        } catch (Exception e) {
            // Any unexpected JWT parsing error — let Security reject it downstream
            log.error("JWT processing error for URI={}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}