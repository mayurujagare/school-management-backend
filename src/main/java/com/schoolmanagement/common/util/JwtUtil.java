package com.schoolmanagement.common.util;

import com.schoolmanagement.module.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-minutes}")
    private int accessTokenExpiryMinutes;

    // ── Generate Access Token ─────────────────────────────────
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("schoolId",  user.getSchoolId() != null
                                ? user.getSchoolId().toString() : null);
        claims.put("roles",     user.getRoles().stream()
                                .map(r -> r.getName())
                                .collect(Collectors.toList()));
        claims.put("firstName", user.getFirstName());
        claims.put("lastName",  user.getLastName());
        claims.put("authType",  user.getAuthType().name());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + (long) accessTokenExpiryMinutes * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Generate Refresh Token (opaque) ──────────────────────
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    // ── Validate & Parse ──────────────────────────────────────
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateAndExtractClaims(token).getSubject());
    }

    public UUID extractSchoolId(String token) {
        String schoolId = validateAndExtractClaims(token).get("schoolId", String.class);
        return schoolId != null ? UUID.fromString(schoolId) : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return validateAndExtractClaims(token).get("roles", List.class);
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndExtractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Hash refresh token before storing in DB ───────────────
    public String hashToken(String rawToken) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}