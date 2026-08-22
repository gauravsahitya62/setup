package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryMovementType;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID variantId,
        int quantity,
        InventoryMovementType movementType,
        String reason,
        UUID referenceId,
        UUID createdBy,
        Instant createdAt
) {
}
