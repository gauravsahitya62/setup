package com.flowset.marketplace.product.dto;

import com.flowset.common.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        String vendorSlug,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        String name,
        String slug,
        String shortDescription,
        String featuredImage,
        BigDecimal pricePerDay,
        ProductStatus status,
        boolean featured,
        boolean verifiedVendor,
        BigDecimal vendorRating,
        int reviewCount,
        String city,
        boolean available,
        String moderationReason,
        Instant createdAt
) {
}
