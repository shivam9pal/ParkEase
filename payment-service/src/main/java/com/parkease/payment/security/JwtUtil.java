package com.parkease.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()           // ← 0.11.5: parserBuilder() not parser()
                .setSigningKey(getSigningKey()) // ← 0.11.5: setSigningKey() not verifyWith()
                .build()
                .parseClaimsJws(token)         // ← 0.11.5: parseClaimsJws() not parseSignedClaims()
                .getBody();                    // ← 0.11.5: getBody() not getPayload()
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public String extractUserId(Claims claims) {
        return claims.get("userId", String.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }
}