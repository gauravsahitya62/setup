package com.flowset.reviews.dto;

import java.util.List;

public record VendorReviewPageResponse(
        List<PublicReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        VendorRatingSummaryResponse summary
) {
}
