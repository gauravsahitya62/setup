package com.flowset.vendor.dto;

import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        UUID ownerUserId,
        String companyName,
        String slug,
        String description,
        String shortDescription,
        String gstNumber,
        String businessType,
        Integer establishedYear,
        String website,
        String email,
        String phone,
        String logoUrl,
        String bannerUrl,
        BigDecimal rating,
        int reviewCount,
        int completedEvents,
        Integer responseTimeMinutes,
        boolean verified,
        VendorStatus status,
        VendorVerificationStatus verificationStatus,
        boolean featured,
        String verificationReason,
        Instant verificationReviewedAt,
        List<ServiceAreaResponse> serviceAreas,
        List<PortfolioItemResponse> portfolio,
        SocialLinksResponse socialLinks,
        List<DocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt
) {
}
