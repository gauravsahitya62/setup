package com.flowset.planner.booking.dto;

import com.flowset.common.BookingRequestStatus;
import com.flowset.common.BookingTimelineEventType;
import java.time.Instant;
import java.util.UUID;

public record BookingTimelineEventResponse(
        UUID id,
        BookingTimelineEventType eventType,
        BookingRequestStatus fromStatus,
        BookingRequestStatus toStatus,
        String description,
        String note,
        UUID actorId,
        Instant createdAt
) {
}
