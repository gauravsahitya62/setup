package com.flowset.marketplace.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequest(
        @NotNull(message = "Total quantity is required")
        @Min(value = 0, message = "Total quantity must be zero or positive")
        Integer totalQuantity,
        @Min(value = 0, message = "Reserved must be zero or positive") Integer reserved,
        @Min(value = 0, message = "Maintenance must be zero or positive") Integer maintenance,
        @Min(value = 0, message = "Damaged must be zero or positive") Integer damaged
) {
}
