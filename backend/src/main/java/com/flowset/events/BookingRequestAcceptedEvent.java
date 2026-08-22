package com.flowset.events;

import java.util.UUID;

public record BookingRequestAcceptedEvent(
        UUID bookingRequestId,
        UUID projectId,
        UUID plannerId,
        UUID vendorId,
        UUID vendorOwnerUserId,
        String projectName,
        String vendorName
) {
}
