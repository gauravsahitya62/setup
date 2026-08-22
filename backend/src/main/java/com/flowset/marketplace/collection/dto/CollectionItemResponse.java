package com.flowset.marketplace.collection.dto;

import java.util.UUID;

public record CollectionItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        int displayOrder
) {
}
