package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "flowset.app")
public record AppEnvironmentProperties(
        @DefaultValue("development") String env,
        @DefaultValue("http://localhost:3000") String frontendUrl
) {
}
