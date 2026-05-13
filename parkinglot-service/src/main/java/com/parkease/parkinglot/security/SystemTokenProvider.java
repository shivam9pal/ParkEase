package com.parkease.parkinglot.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Generates system JWT tokens for service-to-service communication.
 *
 * Used when parkinglot-service needs to call another service (e.g.,
 * notification-service) from a non-HTTP context (e.g., message consumer,
 * scheduled job).
 *
 * Token details: - Subject: "system@parkease.internal" - Role: ADMIN - UserId:
 * 00000000-0000-0000-0000-000000000000 (system user) - Valid for: 1 hour -
 * Signed with: Same JWT_SECRET as auth-service
 */
@Component
@Slf4j
public class SystemTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Generates a system JWT token valid for 1 hour with ADMIN role. Called by
     * FeignConfig when no HTTP request context exists.
     *
     * @return JWT token string
     */
    public String generateSystemToken() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            String token = Jwts.builder()
                    .setSubject("system@parkease.internal")
                    .claim("userId", "00000000-0000-0000-0000-000000000000")
                    .claim("role", "ADMIN")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3_600_000)) // 1 hour
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            log.debug("[SystemTokenProvider] Generated system token for service-to-service call");
            return token;
        } catch (Exception e) {
            log.error("[SystemTokenProvider] Failed to generate system token", e);
            throw new RuntimeException("Failed to generate system token for Feign client", e);
        }
    }
}
