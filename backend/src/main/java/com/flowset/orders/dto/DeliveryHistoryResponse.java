package com.flowset.orders.dto;

import com.flowset.common.DeliveryStatus;
import java.time.Instant;
import java.util.UUID;

public record DeliveryHistoryResponse(
        UUID id,
        DeliveryStatus fromStatus,
        DeliveryStatus toStatus,
        UUID changedBy,
        String note,
        Instant createdAt
) {
}
