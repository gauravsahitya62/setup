package com.flowset.admin.dto;

import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.dto.VendorResponse;
import java.util.List;

public record AdminVendorDetailResponse(
        VendorResponse vendor,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        List<DocumentResponse> documents,
        List<ProductSummaryResponse> products,
        List<ReviewResponse> reviews,
        List<BookingRequestSummaryResponse> bookings
) {
}
