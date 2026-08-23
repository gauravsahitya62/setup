package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "flowset.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("20") int authPerMinute,
        @DefaultValue("60") int searchPerMinute,
        @DefaultValue("10") int supportPerMinute,
        @DefaultValue("30") int adminPerMinute
) {
}
