package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        long accessExpirationMs,
        long refreshExpirationMs,
        String refreshCookieName
) {
}
