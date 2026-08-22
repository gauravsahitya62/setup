package com.flowset.orders.repository;

import com.flowset.common.OrderStatus;
import com.flowset.common.RoleName;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<OrderEntity> list(
            UserPrincipal principal,
            UUID vendorId,
            UUID plannerId,
            UUID projectId,
            OrderStatus status
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
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
