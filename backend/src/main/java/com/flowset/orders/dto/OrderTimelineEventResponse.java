package com.flowset.orders.dto;

import com.flowset.common.OrderStatus;
import com.flowset.common.OrderTimelineEventType;
import java.time.Instant;
import java.util.UUID;

public record OrderTimelineEventResponse(
        UUID id,
        OrderTimelineEventType eventType,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String description,
        String note,
        UUID actorId,
        Instant createdAt
) {
}
