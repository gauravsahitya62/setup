package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AddProjectProductRequest(
        @NotNull(message = "Variant is required") UUID variantId,
        @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
        @Size(max = 2000) String notes
) {
}
