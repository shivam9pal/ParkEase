package com.parkease.spot.security;

import io.jsonwebtoken.JwtException;
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
 * JWT authentication filter for spot-service.
 *
 * <p>Runs once per request (OncePerRequestFilter). Extracts the Bearer token
 * from the Authorization header, validates it via JwtUtil, and populates the
 * SecurityContext so Spring Security can enforce role-based access control.
 *
 * <p>If the token is absent or invalid the filter does NOT abort the request —
 * it lets it pass through unauthenticated. SecurityConfig then rejects it if
 * the endpoint requires authentication.
 *
 * <p>Request flow:
 * <pre>
 *   1. Extract "Authorization: Bearer <token>" header
 *   2. Parse email from token
 *   3. Validate signature + expiry
 *   4. Build UsernamePasswordAuthenticationToken with ROLE_<role> authority
 *   5. Attach userId + role as details map on the authentication object
 *   6. Set authentication into SecurityContextHolder
 *   7. Continue filter chain
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // ── 1. No Bearer header present → skip JWT processing entirely ────────
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Extract raw token ───────────────────────────────────────────────
        final String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // ── 3. Extract claims ──────────────────────────────────────────────
            final String email  = jwtUtil.extractEmail(token);
            final String role   = jwtUtil.extractRole(token);
            final UUID   userId = jwtUtil.extractUserId(token);

            // ── 4. Only set auth if SecurityContext is not already populated ───
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtUtil.isTokenValid(token, email)) {

                    // ── 5. Build authority: role "MANAGER" → "ROLE_MANAGER" ────
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email,      // principal
                                    null,       // credentials — not needed post-validation
                                    authorities
                            );

                    // ── 6. Attach userId + role as details for controller use ──
                    authToken.setDetails(
                            Map.of(
                                    "userId", userId.toString(),
                                    "role",   role
                            )
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Store userId + role in request attributes for easy access
                    request.setAttribute("userId", userId.toString());
                    request.setAttribute("role",   role);

                    // ── 7. Populate SecurityContext ────────────────────────────
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authenticated user [{}] with role [{}]", email, role);

                } else {
                    log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                }
            }

        } catch (JwtException | IllegalArgumentException ex) {
            // Token is malformed / expired / tampered — clear context and proceed
            // SecurityConfig will reject the request if the endpoint is protected
            log.warn("JWT processing error on [{}]: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}