package com.flowset.marketplace.collection.dto;

import com.flowset.common.CollectionStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CollectionRequest(
        UUID vendorId,
        @NotBlank(message = "Name is required") String name,
        String slug,
        String description,
        String featuredImage,
        CollectionStatus status
) {
}
