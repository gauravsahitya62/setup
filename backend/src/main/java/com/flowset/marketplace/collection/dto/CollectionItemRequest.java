package com.flowset.marketplace.collection.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CollectionItemRequest(
        @NotNull(message = "Product id is required") UUID productId,
        Integer displayOrder
) {
}
