package com.flowset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowset.inventory")
public record InventoryProperties(int lowStockThresholdPercent, int maxCalendarDays) {

    public InventoryProperties {
        if (lowStockThresholdPercent <= 0) {
            lowStockThresholdPercent = 20;
        }
        if (maxCalendarDays <= 0) {
            maxCalendarDays = 366;
        }
    }
}
