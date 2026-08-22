package com.flowset.reviews;

import com.flowset.common.ReviewSort;
import com.flowset.common.ReviewStatus;
import com.flowset.reviews.entity.ReviewEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<ReviewEntity> publishedForVendor(UUID vendorId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("vendorId"), vendorId));
            predicates.add(cb.equal(root.get("status"), ReviewStatus.PUBLISHED));
            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<ReviewEntity> overallRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("overallRating"), rating);
    }

    public static Specification<ReviewEntity> verified(Boolean verified) {
        if (verified == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("verified"), verified);
    }

    public static Specification<ReviewEntity> status(ReviewStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ReviewEntity> vendor(UUID vendorId) {
        if (vendorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("vendorId"), vendorId);
    }

    public static Specification<ReviewEntity> planner(UUID plannerId) {
        if (plannerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("plannerId"), plannerId);
    }

    public static org.springframework.data.domain.Sort sort(ReviewSort sort) {
        ReviewSort resolved = sort == null ? ReviewSort.NEWEST : sort;
        return switch (resolved) {
            case HIGHEST -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("overallRating"),
                    org.springframework.data.domain.Sort.Order.desc("publishedAt"));
            case LOWEST -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("overallRating"),
                    org.springframework.data.domain.Sort.Order.desc("publishedAt"));
            case NEWEST -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("publishedAt"),
                    org.springframework.data.domain.Sort.Order.desc("createdAt"));
        };
    }
}
