package com.flowset.planner.projects.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectVendorResponse(
        UUID id,
        UUID projectId,
        UUID vendorId,
        String companyName,
        String slug,
        String logoUrl,
        BigDecimal rating,
        int reviewCount,
        boolean verified,
        String city,
        String notes,
        long productsInProject,
        boolean unavailable,
        Instant createdAt
) {
}
