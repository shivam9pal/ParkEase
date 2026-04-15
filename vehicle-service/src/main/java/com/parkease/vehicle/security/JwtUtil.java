package com.parkease.vehicle.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * READ-ONLY JWT utility for vehicle-service.
 *
 * This service does NOT generate tokens — auth-service owns token generation.
 * vehicle-service only VALIDATES and READS claims from tokens issued by auth-service.
 *
 * MUST use the exact same secret key as auth-service so signatures verify correctly.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // ─────────────────────────────────────────────────────────────────────────
    // KEY
    // ─────────────────────────────────────────────────────────────────────────

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLAIMS EXTRACTION
    // ─────────────────────────────────────────────────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the email (JWT subject) from the token.
     * auth-service sets: .setSubject(email)
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the role claim — e.g., "DRIVER", "MANAGER", "ADMIN".
     * auth-service sets: .claim("role", role)
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extracts the userId claim (UUID as String).
     * auth-service sets: .claim("userId", userId.toString())
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * Extracts the token expiration date.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates token signature AND checks it has not expired.
     * Does NOT check database — vehicle-service is stateless.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // throws if signature invalid
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
}