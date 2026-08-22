package com.flowset.marketplace.product.dto;

import java.util.UUID;

public record AttributeResponse(
        UUID id,
        String attributeName,
        String attributeValue,
        int displayOrder
) {
}
