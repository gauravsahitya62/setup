package com.flowset.planner.booking.dto;

public record VendorBookingStatsResponse(
        long pending,
        long accepted,
        long rejected,
        long cancelled,
        long confirmed
) {
}
