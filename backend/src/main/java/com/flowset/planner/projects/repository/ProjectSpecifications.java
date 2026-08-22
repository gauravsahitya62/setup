package com.flowset.planner.projects.repository;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import com.flowset.common.RoleName;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<ProjectEntity> list(
            UserPrincipal principal,
            String query,
            ProjectStatus status,
            EventType eventType,
            UUID plannerIdFilter
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (principal.getRole() == RoleName.ADMIN) {
                if (plannerIdFilter != null) {
                    predicates.add(cb.equal(root.get("plannerId"), plannerIdFilter));
                }
            } else {
                predicates.add(cb.equal(root.get("plannerId"), principal.getId()));
            }

            if (StringUtils.hasText(query)) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), like));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
