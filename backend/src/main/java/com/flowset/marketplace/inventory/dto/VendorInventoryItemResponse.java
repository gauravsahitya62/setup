package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryStockStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vendor inventory row with product/variant context and computed stock status")
public record VendorInventoryItemResponse(
        UUID id,
        UUID variantId,
        UUID productId,
        String productName,
        String productSlug,
        String featuredImage,
        String variantName,
        String sku,
        String color,
        int totalQuantity,
        int reservedQuantity,
        int availableQuantity,
        int maintenanceQuantity,
        int damagedQuantity,
        InventoryStockStatus status,
        boolean lowStock,
        Instant updatedAt,
        int version
) {
}
