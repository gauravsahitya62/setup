package com.flowset.marketplace.product.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ImageReorderRequest(
        @NotEmpty(message = "Image order list is required") List<UUID> imageIds
) {
}
