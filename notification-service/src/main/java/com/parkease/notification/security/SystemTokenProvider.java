package com.parkease.notification.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class SystemTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ─── Unified Signing Key (MATCH ALL SERVICES) ────────────────────────────

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Generate Internal System Token ─────────────────────────────────────

    public String generateSystemToken() {
        return Jwts.builder()
                .setSubject("system@parkease.internal")
                .claim("userId", "00000000-0000-0000-0000-000000000000")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000)) // 1 hour
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}