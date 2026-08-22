package com.flowset.marketplace.collection.dto;

import com.flowset.common.CollectionStatus;
import java.time.Instant;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        UUID vendorId,
        String name,
        String slug,
        String description,
        String featuredImage,
        CollectionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
