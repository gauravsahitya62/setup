package com.flowset.admin.dto;

import com.flowset.common.AuditResourceType;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorName,
        String action,
        AuditResourceType resourceType,
        UUID resourceId,
        String ipAddress,
        String userAgent,
        String metadata,
        Instant createdAt
) {
}
