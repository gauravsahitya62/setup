package com.flowset.marketplace.inventory.dto;

import com.flowset.common.ReservationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Used by Prompt 13 (booking) and by tests. PENDING holds do not consume stock;
 * CONFIRMED reservations do.
 */
public record ReservationCreateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        UUID bookingRequestId,
        ReservationStatus status
) {
}
