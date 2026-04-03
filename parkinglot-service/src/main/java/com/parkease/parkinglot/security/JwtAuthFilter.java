package com.parkease.parkinglot.security;

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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email  = jwtUtil.extractEmail(token);
            String role   = jwtUtil.extractRole(token);
            UUID   userId = jwtUtil.extractUserId(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (!jwtUtil.isTokenExpired(token)) {

                    // Build authority list — Spring Security expects ROLE_ prefix
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);

                    // Store userId and role as details for retrieval in controller
                    authToken.setDetails(Map.of(
                            "userId", userId.toString(),
                            "role", role
                    ));

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Re-set details to include our custom claims after WebAuthDetails
                    UsernamePasswordAuthenticationToken finalToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    finalToken.setDetails(Map.of(
                            "userId", userId.toString(),
                            "role",   role,
                            "email",  email
                    ));

                    SecurityContextHolder.getContext().setAuthentication(finalToken);
                }
            }
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            // Do NOT send 401 here — let Spring Security handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }
}