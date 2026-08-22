package com.flowset.admin.service;

import com.flowset.admin.dto.AuditLogResponse;
import com.flowset.audit.entity.AuditLogEntity;
import com.flowset.audit.repository.AuditLogRepository;
import com.flowset.common.AuditResourceType;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminAuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminAuditQueryService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            String action,
            AuditResourceType resourceType,
            UUID actorUserId,
            UUID resourceId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        Specification<AuditLogEntity> spec = Specification.where(null);
        if (StringUtils.hasText(action)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (resourceType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), resourceType));
        }
        if (actorUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (resourceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceId"), resourceId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        Page<AuditLogEntity> result = auditLogRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        String name = entity.getActorUserId() == null
                ? "System"
                : userRepository.findById(entity.getActorUserId())
                        .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                        .orElse("User");
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorUserId(),
                name,
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }
}
