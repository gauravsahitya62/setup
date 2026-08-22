package com.flowset.orders.dto;

import com.flowset.common.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID bookingRequestId,
        UUID projectId,
        String projectName,
        UUID vendorId,
        String vendorName,
        String vendorLogoUrl,
        UUID plannerId,
        String plannerName,
        OrderStatus status,
        LocalDate eventDate,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        LocalDate deliveryDate,
        LocalDate pickupDate,
        String itemsSummary,
        int itemCount,
        BigDecimal estimatedSubtotal,
        BigDecimal deliveryFee,
        BigDecimal securityDeposit,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
}
