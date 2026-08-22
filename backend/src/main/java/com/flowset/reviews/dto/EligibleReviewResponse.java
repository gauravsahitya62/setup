package com.flowset.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record EligibleReviewResponse(
        UUID orderId,
        String orderNumber,
        UUID vendorId,
        String vendorName,
        String vendorSlug,
        Instant completedAt,
        Instant eligibleUntil,
        boolean reviewPeriodExpired
) {
}
