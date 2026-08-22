package com.flowset.marketplace.product.dto;

import com.flowset.common.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        String vendorSlug,
        boolean verifiedVendor,
        BigDecimal vendorRating,
        int reviewCount,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        UUID subcategoryId,
        String subcategoryName,
        String name,
        String slug,
        String shortDescription,
        String description,
        String featuredImage,
        BigDecimal pricePerDay,
        BigDecimal securityDeposit,
        int minimumRentalDays,
        boolean deliveryAvailable,
        boolean setupAvailable,
        boolean pickupAvailable,
        ProductStatus status,
        boolean featured,
        List<VariantResponse> variants,
        List<ImageResponse> images,
        List<AttributeResponse> attributes,
        List<String> tags,
        int viewCount,
        String moderationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
