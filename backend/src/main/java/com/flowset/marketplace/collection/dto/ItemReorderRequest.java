package com.flowset.marketplace.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ItemReorderRequest(
        @NotEmpty(message = "Product order list is required") List<UUID> productIds
) {
}
