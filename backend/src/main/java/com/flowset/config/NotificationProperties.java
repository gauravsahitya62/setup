package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.notifications")
public record NotificationProperties(int retentionDays, boolean archiveEnabled) {

    public NotificationProperties {
        if (retentionDays <= 0) {
            retentionDays = 90;
        }
    }
}
