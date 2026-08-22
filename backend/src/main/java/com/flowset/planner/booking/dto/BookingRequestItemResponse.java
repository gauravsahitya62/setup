package com.flowset.planner.booking.dto;

import com.flowset.common.AvailabilityLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingRequestItemResponse(
        UUID id,
        UUID productVariantId,
        String productName,
        String variantName,
        String sku,
        UUID vendorId,
        int quantity,
        BigDecimal pricePerDay,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        int rentalDays,
        BigDecimal lineEstimatedValue,
        String notes,
        AvailabilityLevel availabilityStatus,
        Integer availableQuantity,
        String availabilityMessage
) {
}
