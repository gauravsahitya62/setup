package com.flowset.vendor.dto;

import jakarta.validation.constraints.Size;

public record PortfolioUpdateRequest(
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String description,

        Integer displayOrder
) {
}
