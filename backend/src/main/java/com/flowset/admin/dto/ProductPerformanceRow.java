package com.flowset.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPerformanceRow(
        UUID productId,
        String productName,
        long views,
        long projectAdds,
        long bookingRequests,
        long confirmedBookings,
        long completedOrders,
        BigDecimal estimatedBookingValue,
        long favorites
) {
}
