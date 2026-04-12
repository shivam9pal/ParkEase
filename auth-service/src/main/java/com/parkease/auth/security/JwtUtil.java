package com.parkease.auth.security;

import com.parkease.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiry}")
    private long jwtExpiry;

    // ── Generate token (EXISTING — unchanged) ──────────────────────────────────

    public String generateToken(String email, String role, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return buildToken(claims, email, jwtExpiry);
    }

    // ── Generate Admin token (NEW) ─────────────────────────────────────────────
    // Adds extra isSuperAdmin claim on top of standard role + userId claims.
    // Other services only read role=ADMIN from JWT — isSuperAdmin is ignored by them silently.

    public String generateAdminToken(String email, String adminId, User.Role role, boolean isSuperAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        claims.put("userId", adminId);
        claims.put("isSuperAdmin", isSuperAdmin);
        return buildToken(claims, email, jwtExpiry);
    }

    // ── Shared token builder (EXISTING — unchanged) ────────────────────────────

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Validate token (EXISTING — unchanged) ──────────────────────────────────

    public boolean isTokenValid(String token, String userEmail) {
        final String email = extractEmail(token);
        return (email.equals(userEmail)) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Extract claims (EXISTING — unchanged) ──────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ── Extract isSuperAdmin claim (NEW) ───────────────────────────────────────
    // Returns false safely if claim is absent (e.g. regular user tokens)

    public boolean extractIsSuperAdmin(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("isSuperAdmin");
            if (val == null) return false;
            return Boolean.TRUE.equals(val);
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public long getJwtExpiry() {
        return jwtExpiry;
    }
}
