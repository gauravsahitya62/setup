package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.reviews")
public record ReviewProperties(int editWindowDays, int maxTextLength) {

    public ReviewProperties {
        if (editWindowDays <= 0) {
            editWindowDays = 30;
        }
        if (maxTextLength <= 0) {
            maxTextLength = 2000;
        }
    }
}
