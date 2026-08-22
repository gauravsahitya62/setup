package com.flowset.planner.booking.dto;

import com.flowset.common.BookingRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingRequestSummaryResponse(
        UUID id,
        UUID projectId,
        String projectName,
        UUID vendorId,
        String vendorName,
        String vendorLogoUrl,
        UUID plannerId,
        String plannerName,
        BookingRequestStatus status,
        LocalDate eventDate,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        String itemsSummary,
        int itemCount,
        BigDecimal estimatedRentalValue,
        String currency,
        Instant requestedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
