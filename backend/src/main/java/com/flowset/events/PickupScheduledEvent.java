package com.flowset.events;

import java.time.LocalDate;
import java.util.UUID;

public record PickupScheduledEvent(
        UUID orderId,
        UUID projectId,
        UUID plannerId,
        UUID vendorId,
        UUID vendorOwnerUserId,
        String orderNumber,
        String projectName,
        String vendorName,
        LocalDate pickupDate
) {
}
