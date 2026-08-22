package com.flowset.marketplace.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Standing inventory totals for a product variant")
public record InventoryResponse(
        UUID id,
        UUID variantId,
        int totalQuantity,
        int reserved,
        int available,
        int maintenance,
        int damaged,
        Instant createdAt,
        Instant updatedAt,
        int version
) {
}
