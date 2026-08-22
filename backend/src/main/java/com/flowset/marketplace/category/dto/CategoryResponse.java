package com.flowset.marketplace.category.dto;

import com.flowset.common.CategoryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String icon,
        String imageUrl,
        UUID parentCategoryId,
        int displayOrder,
        boolean featured,
        CategoryStatus status,
        List<CategoryResponse> children,
        Instant createdAt,
        Instant updatedAt
) {
}
