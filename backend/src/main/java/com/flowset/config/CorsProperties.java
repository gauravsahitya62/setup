package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.cors")
public record CorsProperties(String allowedOrigins) {
}
