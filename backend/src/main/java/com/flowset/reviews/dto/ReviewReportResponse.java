package com.flowset.reviews.dto;

import com.flowset.common.ReviewReportReason;
import com.flowset.common.ReviewReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewReportResponse(
        UUID id,
        UUID reviewId,
        UUID reportedBy,
        ReviewReportReason reason,
        String description,
        ReviewReportStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}
