package com.flowset.events;

import java.util.UUID;

public record ReviewPublishedEvent(
        UUID reviewId,
        UUID orderId,
        UUID vendorId,
        UUID plannerId,
        UUID vendorOwnerUserId,
        String vendorName,
        String orderNumber
) {
}
