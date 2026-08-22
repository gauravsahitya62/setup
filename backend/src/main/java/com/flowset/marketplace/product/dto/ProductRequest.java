package com.flowset.marketplace.product.dto;

import com.flowset.common.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
        UUID vendorId,
        @NotBlank(message = "Name is required") String name,
        String slug,
        @NotNull(message = "Category is required") UUID categoryId,
        UUID subcategoryId,
        String shortDescription,
        String description,
        @NotNull(message = "Price per day is required")
        @DecimalMin(value = "0.0", message = "Price per day must be positive")
        BigDecimal pricePerDay,
        @DecimalMin(value = "0.0", message = "Security deposit must be positive")
        BigDecimal securityDeposit,
        @Min(value = 1, message = "Minimum rental days must be at least 1")
        Integer minimumRentalDays,
        Boolean deliveryAvailable,
        Boolean setupAvailable,
        Boolean pickupAvailable,
        Boolean featured,
        ProductStatus status,
        List<UUID> tagIds
) {
}
