package com.parkease.analytics.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class SystemTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ─── 0.11.5 API: .setSubject() .addClaims() .setIssuedAt() .setExpiration() .signWith() ───

    public String generateSystemToken() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject("system@parkease.internal")             // ✅ 0.11.5
                .claim("userId", "00000000-0000-0000-0000-000000000000")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())                            // ✅ 0.11.5
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000)) // ✅ 0.11.5
                .signWith(key, SignatureAlgorithm.HS256)            // ✅ 0.11.5 needs algo
                .compact();
    }
}