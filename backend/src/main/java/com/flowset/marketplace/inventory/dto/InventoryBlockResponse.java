package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryBlockReason;
import com.flowset.common.InventoryBlockStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBlockResponse(
        UUID id,
        UUID variantId,
        int quantity,
        LocalDate startDate,
        LocalDate endDate,
        InventoryBlockReason reason,
        String notes,
        InventoryBlockStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
