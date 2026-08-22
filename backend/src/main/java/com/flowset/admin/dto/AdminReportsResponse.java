package com.flowset.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminReportsResponse(
        List<VendorPerformanceRow> vendorPerformance,
        List<ProductPerformanceRow> productPerformance,
        long bookingRequests,
        long confirmedOrders,
        long completedOrders,
        long cancelledOrders,
        BigDecimal estimatedRentalValue,
        long publishedReviews,
        long reportedReviews,
        long newUsers,
        String financialLabel
) {
}
