package com.flowset.marketplace.inventory.dto;

import com.flowset.common.AvailabilityStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityUpdateRequest(
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Status is required") AvailabilityStatus status,
        @Min(value = 0, message = "Quantity reserved must be positive") Integer quantityReserved,
        String notes
) {
}
