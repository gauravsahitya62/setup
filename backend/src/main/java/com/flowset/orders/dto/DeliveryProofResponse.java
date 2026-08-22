package com.flowset.orders.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryProofResponse(
        UUID id,
        String fileUrl,
        String fileName,
        String fileType,
        Instant uploadedAt,
        UUID uploadedBy
) {
}
