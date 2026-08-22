package com.flowset.audit.repository;

import com.flowset.audit.entity.AuditLogEntity;
import com.flowset.common.AuditResourceType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
        extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {

    long countByActorUserIdAndCreatedAtGreaterThanEqual(UUID actorUserId, Instant from);
}
