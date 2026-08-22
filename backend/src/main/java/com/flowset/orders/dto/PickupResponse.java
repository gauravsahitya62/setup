package com.flowset.orders.dto;

import com.flowset.common.PickupStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PickupResponse(
        UUID id,
        UUID orderId,
        LocalDate pickupDate,
        LocalTime preferredTime,
        Instant actualPickupTime,
        String notes,
        PickupStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
