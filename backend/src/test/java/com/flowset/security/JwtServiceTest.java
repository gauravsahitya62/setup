package com.flowset.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.common.RoleName;
import com.flowset.config.JwtProperties;
import com.flowset.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(
                "test-access-secret-must-be-at-least-32-chars-long!!",
                "test-refresh-secret-must-be-at-least-32-chars-long!",
                900_000,
                604_800_000,
                "flowset_refresh"));
    }

    @Test
    void createsAndParsesAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createAccessToken(userId, "a@example.com", RoleName.PLANNER);
        Claims claims = jwtService.parseAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("a@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("PLANNER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    void rejectsAccessTokenAsRefresh() {
        UUID userId = UUID.randomUUID();
        String access = jwtService.createAccessToken(userId, "a@example.com", RoleName.VENDOR);
        assertThatThrownBy(() -> jwtService.parseRefreshToken(access))
                .isInstanceOf(Exception.class);
    }
}
