package com.flowset.marketplace.product.dto;

import java.util.UUID;

public record ImageResponse(
        UUID id,
        String imageUrl,
        String altText,
        int displayOrder,
        boolean featured
) {
}
