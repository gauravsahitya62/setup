package com.flowset.reviews.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VendorRatingSummaryResponse(
        BigDecimal averageRating,
        long reviewCount,
        long verifiedCount,
        Map<Integer, Long> distribution
) {
}
