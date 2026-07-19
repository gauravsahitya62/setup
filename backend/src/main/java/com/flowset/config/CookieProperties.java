package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.cookie")
public record CookieProperties(boolean secure, String sameSite) {
}
