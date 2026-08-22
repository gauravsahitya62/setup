package com.flowset.marketplace.product.dto;

import com.flowset.common.VariantStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        UUID productId,
        String variantName,
        String sku,
        String color,
        String material,
        int quantity,
        int availableQuantity,
        BigDecimal pricePerDay,
        VariantStatus status
) {
}
