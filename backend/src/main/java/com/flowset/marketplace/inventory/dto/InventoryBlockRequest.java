package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryBlockReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InventoryBlockRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        @NotNull(message = "Reason is required") InventoryBlockReason reason,
        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes
) {
}
