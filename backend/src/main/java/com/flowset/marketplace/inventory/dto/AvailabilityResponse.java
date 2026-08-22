package com.flowset.marketplace.inventory.dto;

import com.flowset.common.AvailabilityStatus;
import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        UUID variantId,
        LocalDate date,
        AvailabilityStatus status,
        int quantityReserved,
        String notes
) {
}
