package com.flowset.events;

import com.flowset.common.ReviewReportReason;
import java.util.UUID;

public record ReviewReportedEvent(
        UUID reviewId,
        UUID vendorId,
        UUID reportedBy,
        ReviewReportReason reason
) {
}
