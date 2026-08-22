package com.flowset.marketplace.inventory.dto;

import com.flowset.common.MaintenanceRecordStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceRecordResponse(
        UUID id,
        UUID variantId,
        UUID blockId,
        int quantity,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        MaintenanceRecordStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
