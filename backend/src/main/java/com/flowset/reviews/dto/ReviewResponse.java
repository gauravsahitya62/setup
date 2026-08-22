package com.flowset.reviews.dto;

import com.flowset.common.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID orderId,
        UUID vendorId,
        String vendorName,
        UUID plannerId,
        String plannerDisplayName,
        String plannerInitials,
        Integer overallRating,
        Integer communicationRating,
        Integer productQualityRating,
        Integer deliveryRating,
        Integer valueRating,
        String reviewText,
        ReviewStatus status,
        boolean verified,
        boolean canEdit,
        Instant editExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant deletedAt
) {
}
