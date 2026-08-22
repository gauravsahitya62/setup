package com.flowset.reviews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flowset.reviews.dto.VendorRatingSummaryResponse;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VendorRatingServiceTest {

    @Test
    void averagesIntegerStarsAndIgnoresEmptyVendors() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        VendorRepository vendors = mock(VendorRepository.class);
        VendorRatingService service = new VendorRatingService(reviews, vendors);
        UUID vendorId = UUID.randomUUID();

        when(reviews.countPublishedByRating(vendorId)).thenReturn(List.of(
                new Object[] {5, 250L},
                new Object[] {4, 50L},
                new Object[] {3, 10L},
                new Object[] {2, 8L},
                new Object[] {1, 6L}));
        when(reviews.countPublishedVerified(vendorId)).thenReturn(300L);

        VendorRatingSummaryResponse summary = service.summarize(vendorId);

        assertThat(summary.reviewCount()).isEqualTo(324);
        assertThat(summary.verifiedCount()).isEqualTo(300);
        assertThat(summary.averageRating()).isEqualByComparingTo(new BigDecimal("4.64"));
        assertThat(summary.distribution().get(5)).isEqualTo(250L);
        assertThat(summary.distribution().get(1)).isEqualTo(6L);
    }

    @Test
    void zeroPublishedReviewsYieldZeroAverage() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        VendorRatingService service = new VendorRatingService(reviews, mock(VendorRepository.class));
        UUID vendorId = UUID.randomUUID();
        when(reviews.countPublishedByRating(vendorId)).thenReturn(List.of());
        when(reviews.countPublishedVerified(vendorId)).thenReturn(0L);

        VendorRatingSummaryResponse summary = service.summarize(vendorId);

        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.averageRating()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(summary.distribution().values()).containsOnly(0L);
    }
}
