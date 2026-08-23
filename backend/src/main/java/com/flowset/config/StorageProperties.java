package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "flowset.storage")
public record StorageProperties(
        @DefaultValue("local") String provider,
        @DefaultValue("./uploads") String localRoot,
        @DefaultValue("http://localhost:8080/api/v1/files") String publicBaseUrl,
        String s3Bucket,
        String s3Region,
        String s3Endpoint,
        @DefaultValue("900") int signedUrlTtlSeconds
) {
}
