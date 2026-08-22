package com.flowset.events;

import com.flowset.common.ReviewModerationActionType;
import java.util.UUID;

public record ReviewModeratedEvent(
        UUID reviewId,
        UUID plannerId,
        UUID vendorOwnerUserId,
        String vendorName,
        ReviewModerationActionType action
) {
}
