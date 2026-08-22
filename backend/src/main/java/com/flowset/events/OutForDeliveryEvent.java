package com.flowset.events;

import java.util.UUID;

public record OutForDeliveryEvent(
        UUID orderId,
        UUID projectId,
        UUID plannerId,
        UUID vendorOwnerUserId,
        String orderNumber,
        String projectName,
        String vendorName
) {
}
