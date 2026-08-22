package com.flowset.events;

import java.util.UUID;

public record BookingRequestCreatedEvent(
        UUID bookingRequestId,
        UUID projectId,
        UUID plannerId,
        UUID vendorId,
        UUID vendorOwnerUserId,
        String projectName,
        String vendorName,
        String itemSummary
) {
}
