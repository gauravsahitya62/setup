package com.flowset.planner.projects.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectProductResponse(
        UUID id,
        UUID projectId,
        UUID productVariantId,
        UUID productId,
        String productSlug,
        String productName,
        String variantName,
        String sku,
        UUID vendorId,
        String vendorName,
        String vendorSlug,
        boolean vendorVerified,
        int quantity,
        String notes,
        BigDecimal snapshotPricePerDay,
        BigDecimal currentPricePerDay,
        String currency,
        String imageUrl,
        boolean listed,
        boolean deletedFromCatalog,
        Instant createdAt,
        Instant updatedAt
) {
}
