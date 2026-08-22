package com.flowset.marketplace.search;

import com.flowset.common.AvailabilityStatus;
import com.flowset.common.ProductStatus;
import com.flowset.common.VariantStatus;
import com.flowset.marketplace.inventory.entity.AvailabilityCalendarEntity;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Isolated JPA {@link Specification} building blocks for product search. Kept independent from the
 * controller/service layer so a future Elasticsearch-backed {@code ProductSearchPort} implementation can
 * replace {@link JpaProductSearchAdapter} without touching callers.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.PUBLISHED);
    }

    public static Specification<ProductEntity> vendorNotSuspended() {
        return (root, query, cb) -> cb.notEqual(
                root.get("vendor").get("status"), com.flowset.common.VendorStatus.SUSPENDED);
    }

    public static Specification<ProductEntity> vendorNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("vendor").get("deletedAt"));
    }

    public static Specification<ProductEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<ProductEntity> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("shortDescription"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern));
    }

    public static Specification<ProductEntity> categoryId(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<ProductEntity> subcategoryId(UUID subcategoryId) {
        if (subcategoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("subcategory").get("id"), subcategoryId);
    }

    public static Specification<ProductEntity> vendorId(UUID vendorId) {
        if (vendorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<ProductEntity> city(String city) {
        if (!StringUtils.hasText(city)) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            var join = root.join("vendor").join("serviceAreas", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(cb.lower(join.get("city")), city.trim().toLowerCase());
        };
    }

    public static Specification<ProductEntity> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ProductEntity> verifiedVendor(Boolean verified) {
        if (verified == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("vendor").get("verified"), verified);
    }

    public static Specification<ProductEntity> minRating(BigDecimal minRating) {
        if (minRating == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("vendor").<BigDecimal>get("rating"), minRating);
    }

    /**
     * A product is considered available in the window when it owns at least one ACTIVE variant that has no
     * BLOCKED/MAINTENANCE calendar entry inside [from, to]. This is a pragmatic MVP heuristic documented in
     * {@code docs/search.md} pending a dedicated availability aggregate.
     */
    public static Specification<ProductEntity> availableBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            var variant = subquery.from(ProductVariantEntity.class);
            subquery.select(variant.get("id"));

            Subquery<Long> blockedSubquery = subquery.subquery(Long.class);
            var calendar = blockedSubquery.from(AvailabilityCalendarEntity.class);
            blockedSubquery.select(cb.literal(1L));
            blockedSubquery.where(
                    cb.equal(calendar.get("variant"), variant),
                    cb.between(calendar.get("date"), from, to),
                    calendar.<AvailabilityStatus>get("status").in(
                            AvailabilityStatus.BLOCKED, AvailabilityStatus.MAINTENANCE));

            subquery.where(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("status"), VariantStatus.ACTIVE),
                    cb.not(cb.exists(blockedSubquery)));

            return cb.exists(subquery);
        };
    }

    public static Specification<ProductEntity> allOf(Specification<ProductEntity>... specs) {
        Specification<ProductEntity> combined = Specification.where(null);
        for (Specification<ProductEntity> spec : specs) {
            if (spec != null) {
                combined = combined.and(spec);
            }
        }
        return combined;
    }
}
