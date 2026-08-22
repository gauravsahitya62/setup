package com.flowset.events;

import java.util.UUID;

public record InventoryLowEvent(
        UUID vendorId,
        UUID vendorOwnerUserId,
        UUID variantId,
        UUID inventoryId,
        String productName,
        int availableQuantity
) {
}
