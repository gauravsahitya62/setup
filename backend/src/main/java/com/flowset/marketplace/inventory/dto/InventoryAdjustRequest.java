package com.flowset.marketplace.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustRequest(
        @NotNull(message = "Adjustment quantity is required")
        Integer delta,
        @NotBlank(message = "A reason is required for every inventory adjustment")
        @Size(max = 500, message = "Reason must be 500 characters or fewer")
        String reason
) {
}
