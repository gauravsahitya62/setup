package com.flowset.marketplace.category.mapper;

import com.flowset.marketplace.category.dto.CategoryResponse;
import com.flowset.marketplace.category.entity.CategoryEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(CategoryEntity category) {
        return toResponse(category, List.of());
    }

    public CategoryResponse toResponse(CategoryEntity category, List<CategoryResponse> children) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIcon(),
                category.getImageUrl(),
                category.getParentCategory() == null ? null : category.getParentCategory().getId(),
                category.getDisplayOrder(),
                category.isFeatured(),
                category.getStatus(),
                children,
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
