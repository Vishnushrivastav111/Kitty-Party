package com.microvault.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final SecretKey key;
    private final long expirationHours;
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    public TokenStore(@Value("${microvault.jwt.secret}") String secret,
                      @Value("${microvault.jwt.expiration-hours:12}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String issue(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public UUID requireUserId(String authorization) {
        Claims claims = parse(authorization);
        if (claims == null || claims.getId() == null || revoked.contains(claims.getId())) {
            return null;
        }
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void revoke(String authorization) {
        Claims claims = parse(authorization);
        if (claims != null && claims.getId() != null) {
            revoked.add(claims.getId());
        }
    }

    private Claims parse(String authorization) {
        String token = readToken(authorization);
        if (token.isEmpty()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private String readToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }
}
