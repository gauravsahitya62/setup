package com.flowset.orders.dto;

import com.flowset.common.DeliveryStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        LocalDate deliveryDate,
        LocalTime preferredTime,
        Instant actualDeliveryTime,
        String venueName,
        String venueAddress,
        String city,
        String state,
        String contactPerson,
        String contactPhone,
        String deliveryNotes,
        DeliveryStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryProofResponse> proofs
) {
}
