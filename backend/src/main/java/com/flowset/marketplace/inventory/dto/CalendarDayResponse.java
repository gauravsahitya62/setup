package com.flowset.marketplace.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Per-day occupancy for a variant or an aggregated vendor calendar")
public record CalendarDayResponse(
        LocalDate date,
        UUID variantId,
        String productName,
        String variantName,
        int totalQuantity,
        int reservedQuantity,
        int maintenanceQuantity,
        int damagedQuantity,
        int blockedQuantity,
        int availableQuantity
) {
}
