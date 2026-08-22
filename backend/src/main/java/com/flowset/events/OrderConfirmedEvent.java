package com.flowset.events;

import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        UUID bookingRequestId,
        UUID projectId,
        UUID plannerId,
        UUID vendorId,
        UUID vendorOwnerUserId,
        String orderNumber,
        String projectName,
        String vendorName
) {
}
