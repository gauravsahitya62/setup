package com.flowset.planner.booking.repository;

import com.flowset.common.BookingRequestStatus;
import com.flowset.common.RoleName;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class BookingRequestSpecifications {

    private BookingRequestSpecifications() {
    }

    public static Specification<BookingRequestEntity> list(
            UserPrincipal principal,
            UUID vendorId,
            UUID plannerId,
            UUID projectId,
            BookingRequestStatus status,
            String query
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (principal.getRole() == RoleName.PLANNER) {
                predicates.add(cb.equal(root.get("plannerId"), principal.getId()));
            } else if (principal.getRole() == RoleName.VENDOR) {
                if (vendorId != null) {
                    predicates.add(cb.equal(root.get("vendorId"), vendorId));
                }
            } else if (principal.getRole() == RoleName.ADMIN) {
                if (plannerId != null) {
                    predicates.add(cb.equal(root.get("plannerId"), plannerId));
                }
                if (vendorId != null) {
                    predicates.add(cb.equal(root.get("vendorId"), vendorId));
                }
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(query)) {
                // name search is applied in the service after join; keep id-safe filter here
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
