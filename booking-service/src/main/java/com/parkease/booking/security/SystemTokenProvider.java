package com.parkease.booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Generates short-lived system JWTs for scheduler-triggered Feign calls.
 *
 * WHY THIS EXISTS:
 *   @Scheduled threads have no HTTP request context — no user, no JWT.
 *   Downstream services (spot-service, parkinglot-service) require a valid
 *   JWT on every endpoint. Without this, auto-expiry Feign calls return 401
 *   silently — spots stay stuck RESERVED/OCCUPIED forever.
 *
 * HOW IT WORKS:
 *   Uses the same shared JWT_SECRET already configured in application.yaml.
 *   Signs a fresh token with role=ADMIN valid for 1 hour.
 *   Downstream services validate it identically to any user token —
 *   same secret, same algorithm, same claims structure.
 *
 * SECURITY:
 *   Token is generated in-memory, never stored, never exposed via API.
 *   Valid for 1 hour only — minimal blast radius if intercepted.
 *   Subject is "system@parkease.internal" — identifiable in logs.
 */
@Component
@Slf4j
public class SystemTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 1 hour — more than enough for any scheduler operation
    private static final long SYSTEM_TOKEN_EXPIRY_MS = 3_600_000L;

    /**
     * Generates a fresh system JWT signed with the shared JWT_SECRET.
     *
     * Called by FeignConfig ONLY when RequestContextHolder returns null
     * (i.e., scheduler thread — not a user-initiated HTTP request).
     *
     * Token claims:
     *   sub    → "system@parkease.internal"
     *   userId → random UUID (no real user — system identity)
     *   role   → "ADMIN" (needed to pass role checks in downstream services)
     *   iat    → now
     *   exp    → now + 1 hour
     */
    public String generateSystemToken() {
        Key signingKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
                .setSubject("system@parkease.internal")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + SYSTEM_TOKEN_EXPIRY_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        log.debug("[SystemTokenProvider] Generated fresh system token for scheduler Feign call.");
        return token;
    }
}