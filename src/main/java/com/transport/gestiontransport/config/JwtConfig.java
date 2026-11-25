package com.transport.gestiontransport.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtConfig {

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long expiration;

    public JwtConfig(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        if (expiration <= 0) {
            throw new IllegalArgumentException("La durée de validité JWT doit être positive.");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32 || secret.chars().distinct().count() < 16) {
            throw new IllegalArgumentException("JWT_SECRET doit contenir au moins 32 octets et 16 caractères distincts.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.parser = Jwts.parser().verifyWith(signingKey).build();
        this.expiration = expiration;
    }

    public String generateToken(String subject, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey)
                .compact();
    }

    Optional<Claims> readVerifiedClaims(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            if (subject == null || subject.isBlank() || role == null || role.isBlank() || claims.getExpiration() == null) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
