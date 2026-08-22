package com.flowset.marketplace.inventory.dto;

import com.flowset.common.ReservationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID variantId,
        UUID bookingRequestId,
        int quantity,
        LocalDate startDate,
        LocalDate endDate,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
