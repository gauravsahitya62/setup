package com.flowset.marketplace.inventory.dto;

import com.flowset.common.AvailabilityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Date-window availability for a variant")
public record AvailabilityCheckResponse(
        UUID variantId,
        boolean available,
        AvailabilityLevel status,
        int requestedQuantity,
        int availableQuantity,
        String message,
        List<AvailabilityConflict> conflicts,
        List<CalendarDayResponse> calendar
) {
    public record AvailabilityConflict(
            LocalDate date,
            int reservedQuantity,
            int blockedQuantity,
            int availableQuantity,
            String type
    ) {
    }
}
