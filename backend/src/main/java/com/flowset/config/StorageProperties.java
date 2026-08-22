package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.storage")
public record StorageProperties(
        String provider,
        String localRoot,
        String publicBaseUrl
) {
}
