package com.flowset.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productVariantId,
        String productName,
        String variantName,
        String sku,
        int quantity,
        BigDecimal pricePerDay,
        int rentalDays,
        BigDecimal subtotal,
        Instant createdAt
) {
}
