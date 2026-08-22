package com.flowset.reviews.service;

import com.flowset.reviews.dto.VendorRatingSummaryResponse;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculates {@code vendors.rating} and {@code vendors.review_count} from published reviews.
 * MVP uses database aggregation of integer star ratings. Cached NUMERIC(3,2) on the vendor row
 * keeps marketplace cards cheap; introduce a dedicated aggregate table only if this becomes hot.
 */
@Service
public class VendorRatingService {

    private final ReviewRepository reviewRepository;
    private final VendorRepository vendorRepository;

    public VendorRatingService(ReviewRepository reviewRepository, VendorRepository vendorRepository) {
        this.reviewRepository = reviewRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorRatingSummaryResponse recalculate(UUID vendorId) {
        VendorRatingSummaryResponse summary = summarize(vendorId);
        vendorRepository.findActiveById(vendorId).ifPresent(vendor -> apply(vendor, summary));
        return summary;
    }

    @Transactional(readOnly = true)
    public VendorRatingSummaryResponse summarize(UUID vendorId) {
        List<Object[]> rows = reviewRepository.countPublishedByRating(vendorId);
        Map<Integer, Long> distribution = emptyDistribution();
        long total = 0;
        long weighted = 0;
        for (Object[] row : rows) {
            int stars = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (stars < 1 || stars > 5) {
                continue;
            }
            distribution.put(stars, count);
            total += count;
            weighted += (long) stars * count;
        }
        BigDecimal average = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(weighted).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        long verified = reviewRepository.countPublishedVerified(vendorId);
        return new VendorRatingSummaryResponse(average, total, verified, distribution);
    }

    private void apply(VendorEntity vendor, VendorRatingSummaryResponse summary) {
        vendor.setRating(summary.averageRating());
        vendor.setReviewCount((int) Math.min(summary.reviewCount(), Integer.MAX_VALUE));
        vendorRepository.save(vendor);
    }

    static Map<Integer, Long> emptyDistribution() {
        Map<Integer, Long> map = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            map.put(stars, 0L);
        }
        return map;
    }
}
