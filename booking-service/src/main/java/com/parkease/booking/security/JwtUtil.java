package com.parkease.booking.security;

import io.jsonwebtoken.Claims;
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
 * READ-ONLY JWT utility for booking-service.
 *
 * This service does NOT generate tokens — that is auth-service's responsibility.
 * It only validates and extracts claims from tokens issued by auth-service.
 *
 * CRITICAL: jwt.secret MUST be byte-for-byte identical to the secret in:
 *   auth-service, vehicle-service, parkinglot-service, spot-service.
 * Any mismatch causes signature verification failure → 401 on every request.
 *
 * JWT Structure (issued by auth-service):
 *   Header:  { alg: HS256, typ: JWT }
 *   Payload: {
 *     sub:    "user@email.com",     ← email
 *     userId: "uuid-string",        ← UUID as String
 *     role:   "DRIVER",             ← DRIVER / MANAGER / ADMIN
 *     iat:    1712145045,
 *     exp:    1712231445
 *   }
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ─── Key Builder ──────────────────────────────────────────────────────────

    /**
     * Builds the signing key from the shared secret.
     * Must match the key used in auth-service's JwtUtil.generateToken().
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Claims Extraction ────────────────────────────────────────────────────

    /**
     * Parses and returns all claims from the token.
     * Throws JwtException if signature is invalid or token is expired.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the email (subject) from the token.
     * Used by JwtAuthFilter to load UserDetails and populate SecurityContext.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the role claim — "DRIVER", "MANAGER", or "ADMIN".
     * Used by SecurityConfig for role-based endpoint authorization.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extracts the userId claim as UUID.
     * CRITICAL — this is the userId injected into service methods.
     * auth-service stores it as a String; we parse it back to UUID here.
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    /**
     * Returns true if token is not expired.
     * Called inside JwtAuthFilter before setting SecurityContext.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Full token validation:
     *   1. Email from token matches the provided email
     *   2. Token is not expired
     *
     * @param token  Raw JWT string (without "Bearer " prefix)
     * @param email  Email to validate against token subject
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(email) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("[JwtUtil] Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}