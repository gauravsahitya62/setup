package com.flowset.marketplace.product.dto;

import jakarta.validation.constraints.NotBlank;

public record AttributeRequest(
        @NotBlank(message = "Attribute name is required") String attributeName,
        @NotBlank(message = "Attribute value is required") String attributeValue,
        Integer displayOrder
) {
}
