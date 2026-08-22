package com.flowset.events;

import java.util.UUID;

public record InventoryUnavailableEvent(
        UUID bookingRequestId,
        UUID projectId,
        UUID plannerId,
        String projectName,
        String vendorName
) {
}
