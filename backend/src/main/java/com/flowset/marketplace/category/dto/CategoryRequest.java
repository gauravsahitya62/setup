package com.flowset.marketplace.category.dto;

import com.flowset.common.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Name is required") String name,
        String slug,
        String description,
        String icon,
        String imageUrl,
        UUID parentCategoryId,
        Integer displayOrder,
        CategoryStatus status,
        Boolean featured
) {
}
