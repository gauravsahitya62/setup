package com.flowset.marketplace.product.dto;

import com.flowset.common.VariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VariantRequest(
        @NotBlank(message = "Variant name is required") String variantName,
        @NotBlank(message = "SKU is required") String sku,
        String color,
        String material,
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be positive")
        Integer quantity,
        @DecimalMin(value = "0.0", message = "Price per day must be positive")
        BigDecimal pricePerDay,
        VariantStatus status
) {
}
