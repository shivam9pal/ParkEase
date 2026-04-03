package com.parkease.booking.security;

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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request before the controller.
 *
 * Flow:
 *   1. Extract "Bearer <token>" from Authorization header
 *   2. Parse and validate JWT using JwtUtil
 *   3. Extract email, role, userId from claims
 *   4. Populate SecurityContext with UsernamePasswordAuthenticationToken
 *      → principal  = email (String)
 *      → authority  = "ROLE_DRIVER" / "ROLE_MANAGER" / "ROLE_ADMIN"
 *      → details    = Map { "userId": UUID, "role": String }
 *   5. Pass request to next filter
 *
 * No database lookup — purely token-based, stateless validation.
 * Invalid/missing token: request continues unauthenticated.
 * SecurityConfig then rejects it at the endpoint level with 401.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header or not Bearer → pass through unauthenticated
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix
        final String token = authHeader.substring(7);

        try {
            // Extract claims
            String email  = jwtUtil.extractEmail(token);
            String role   = jwtUtil.extractRole(token);
            UUID   userId = jwtUtil.extractUserId(token);

            // Only set authentication if not already set and token is valid
            if (email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtUtil.isTokenValid(token, email)) {

                // Build authority — Spring Security expects "ROLE_" prefix
                // e.g., "DRIVER" → "ROLE_DRIVER" for hasRole("DRIVER") in SecurityConfig
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());

                // Build authentication token
                // principal = email, credentials = null (stateless), authorities = [ROLE_X]
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(authority)
                        );

                // Attach userId and role as details — extracted in controller via:
                // ((Map<?, ?>) authentication.getDetails()).get("userId")
                authToken.setDetails(
                        Map.of(
                                "userId", userId,
                                "role", role
                        )
                );

                // WebAuthenticationDetailsSource adds request metadata (IP, session)
                authToken.setDetails(
                        new WebAuthenticationDetailsSourceWrapper(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request),
                                userId,
                                role
                        )
                );

                // Set in SecurityContext — from this point the request is authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("[JwtAuthFilter] Authenticated userId={}, role={}, path={}",
                        userId, role, request.getRequestURI());
            }

        } catch (Exception e) {
            // Invalid signature, expired token, malformed JWT → log and continue
            // SecurityConfig will reject the unauthenticated request at endpoint level
            log.warn("[JwtAuthFilter] JWT processing failed for path={}: {}",
                    request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // ─── Inner Helper: Details Wrapper ───────────────────────────────────────

    /**
     * Attaches userId and role to the authentication token's details.
     * Controller extracts these via:
     *
     *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *   WebAuthenticationDetailsWrapper details = (WebAuthenticationDetailsWrapper) auth.getDetails();
     *   UUID userId = details.getUserId();
     *   String role = details.getRole();
     *
     * This avoids re-parsing the JWT in every controller method.
     */
    public static class WebAuthenticationDetailsSourceWrapper
            extends org.springframework.security.web.authentication.WebAuthenticationDetails {

        private final UUID userId;
        private final String role;

        public WebAuthenticationDetailsSourceWrapper(
                org.springframework.security.web.authentication.WebAuthenticationDetails delegate,
                UUID userId,
                String role) {
            super(delegate.getRemoteAddress(), delegate.getSessionId());
            this.userId = userId;
            this.role   = role;
        }

        public UUID getUserId() { return userId; }
        public String getRole() { return role; }
    }
}