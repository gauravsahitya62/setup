package com.flowset.reviews.dto;

import com.flowset.common.ReviewModerationActionType;
import com.flowset.common.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewModerationActionResponse(
        UUID id,
        UUID reviewId,
        UUID actorId,
        ReviewModerationActionType action,
        String note,
        ReviewStatus previousStatus,
        ReviewStatus newStatus,
        Instant createdAt
) {
}
