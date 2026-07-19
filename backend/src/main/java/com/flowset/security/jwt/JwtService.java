package com.flowset.security.jwt;

import com.flowset.common.RoleName;
import com.flowset.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.accessKey = Keys.hmacShaKeyFor(properties.accessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId, String email, RoleName role) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.accessExpirationMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(accessKey)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.refreshExpirationMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, accessKey);
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token, refreshKey);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        return claims;
    }

    public long getAccessExpirationMs() {
        return properties.accessExpirationMs();
    }

    public long getRefreshExpirationMs() {
        return properties.refreshExpirationMs();
    }

    public String getRefreshCookieName() {
        return properties.refreshCookieName();
    }

    private Claims parse(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
