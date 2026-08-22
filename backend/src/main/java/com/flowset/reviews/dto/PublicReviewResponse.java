package com.flowset.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicReviewResponse(
        UUID id,
        UUID vendorId,
        String plannerDisplayName,
        String plannerInitials,
        Integer overallRating,
        Integer communicationRating,
        Integer productQualityRating,
        Integer deliveryRating,
        Integer valueRating,
        String reviewText,
        boolean verified,
        Instant createdAt,
        Instant publishedAt
) {
}
