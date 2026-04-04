package com.parkease.notification.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Generates a short-lived system JWT (role=ADMIN) for Feign calls made from
 * RabbitMQ consumer threads, which have no HTTP context / RequestContextHolder.
 */
@Component
@Slf4j
public class SystemTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ✅ 0.11.5 API — setSubject() + setIssuedAt() + setExpiration() + signWith()
    public String generateSystemToken() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject("system@parkease.internal")
                .claim("userId", "00000000-0000-0000-0000-000000000000")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}