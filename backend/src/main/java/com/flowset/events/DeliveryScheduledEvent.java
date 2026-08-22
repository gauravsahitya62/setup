package com.flowset.events;

import java.time.LocalDate;
import java.util.UUID;

public record DeliveryScheduledEvent(
        UUID orderId,
        UUID projectId,
        UUID plannerId,
        UUID vendorId,
        UUID vendorOwnerUserId,
        String orderNumber,
        String projectName,
        String vendorName,
        LocalDate deliveryDate
) {
}
