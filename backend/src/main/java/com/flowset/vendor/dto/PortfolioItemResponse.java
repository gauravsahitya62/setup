package com.flowset.vendor.dto;

import java.util.UUID;

public record PortfolioItemResponse(
        UUID id,
        String imageUrl,
        String title,
        String description,
        int displayOrder
) {
}
