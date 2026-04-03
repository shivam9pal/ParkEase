package com.parkease.spot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Read-only JWT utility for spot-service.
 *
 * <p>spot-service NEVER generates tokens — that is auth-service's responsibility.
 * This class only parses and validates tokens signed by auth-service using the
 * shared JWT_SECRET. The secret MUST be identical across all services.
 *
 * <p>Token claims structure (set by auth-service):
 * <pre>
 *   sub     → user email
 *   userId  → UUID string
 *   role    → DRIVER | MANAGER | ADMIN
 *   iat     → issued-at timestamp
 *   exp     → expiry timestamp
 * </pre>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ─────────────────────────────── Key Builder ─────────────────────────────

    /**
     * Builds HMAC-SHA256 signing key from the shared secret.
     * Called once per validation — secret is injected from application.yaml.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────────── Claims Extraction ───────────────────────

    /**
     * Parses and returns all claims from the token.
     * Throws JwtException (handled by JwtAuthFilter) on invalid/expired tokens.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the subject — which is the user's email address.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the userId claim set by auth-service during token generation.
     * Returns UUID parsed from the string claim value.
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * Extracts the role claim — DRIVER, MANAGER, or ADMIN.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ─────────────────────────────── Validation ──────────────────────────────

    /**
     * Returns true if the token is structurally valid, not expired,
     * and the subject matches the provided email.
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(email) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the token's expiration date is before now.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}