package com.flowset.events;

import java.util.UUID;

public record OrderCompletedEvent(
        UUID orderId,
        UUID projectId,
        UUID plannerId,
        UUID vendorOwnerUserId,
        String orderNumber,
        String projectName,
        String vendorName
) {
}
