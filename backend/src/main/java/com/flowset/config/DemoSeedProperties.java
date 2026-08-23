package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "flowset.demo-seed")
public record DemoSeedProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("DemoPassw0rd!") String password
) {
}
