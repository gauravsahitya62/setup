package com.flowset.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorPerformanceRow(
        UUID vendorId,
        String companyName,
        long bookingRequests,
        double acceptanceRate,
        double rejectionRate,
        long completedOrders,
        double cancellationRate,
        BigDecimal averageRating,
        Double averageResponseHours,
        BigDecimal estimatedBookingValue
) {
}
