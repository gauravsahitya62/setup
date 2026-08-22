package com.flowset.audit;

import com.flowset.audit.entity.AuditLogEntity;
import com.flowset.audit.repository.AuditLogRepository;
import com.flowset.common.AuditResourceType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an immutable audit row in the caller's transaction. There is no update or delete API.
     */
        @Transactional(propagation = Propagation.REQUIRED)
    public void log(String action, String actorId, String target, String detail) {
        log.info("AUDIT action={} actor={} target={} detail={}", action, actorId, target, detail);
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAction(action == null ? "UNKNOWN" : action);
        entity.setActorUserId(parseUuid(actorId));
        entity.setResourceId(parseUuid(target));
        entity.setResourceType(inferResourceType(action));
        entity.setMetadata(detail);
        applyRequestContext(entity);
        auditLogRepository.save(entity);
    }

        @Transactional(propagation = Propagation.REQUIRED)
    public void record(
            String action,
            UUID actorUserId,
            AuditResourceType resourceType,
            UUID resourceId,
            String metadata
    ) {
        log.info("AUDIT action={} actor={} type={} resource={} detail={}",
                action, actorUserId, resourceType, resourceId, metadata);
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAction(action);
        entity.setActorUserId(actorUserId);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setMetadata(metadata);
        applyRequestContext(entity);
        auditLogRepository.save(entity);
    }

    private void applyRequestContext(AuditLogEntity entity) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        if (request == null) {
            return;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        entity.setIpAddress(trim(ip, 64));
        entity.setUserAgent(trim(request.getHeader("User-Agent"), 512));
    }

    private static AuditResourceType inferResourceType(String action) {
        if (action == null) {
            return null;
        }
        if (action.startsWith("VENDOR") || action.startsWith("PROFILE") || action.startsWith("LOGO")
                || action.startsWith("BANNER") || action.startsWith("PORTFOLIO")) {
            return AuditResourceType.VENDOR;
        }
        if (action.startsWith("DOCUMENT")) {
            return AuditResourceType.VENDOR_DOCUMENT;
        }
        if (action.startsWith("PRODUCT")) {
            return AuditResourceType.PRODUCT;
        }
        if (action.startsWith("CATEGORY")) {
            return AuditResourceType.CATEGORY;
        }
        if (action.startsWith("REVIEW")) {
            return AuditResourceType.REVIEW;
        }
        if (action.startsWith("USER") || action.startsWith("PASSWORD")) {
            return AuditResourceType.USER;
        }
        if (action.startsWith("SUPPORT") || action.startsWith("TICKET")) {
            return AuditResourceType.SUPPORT_TICKET;
        }
        if (action.startsWith("ORDER") || action.startsWith("BOOKING")) {
            return action.startsWith("ORDER") ? AuditResourceType.ORDER : AuditResourceType.BOOKING_REQUEST;
        }
        if (action.startsWith("SETTINGS") || action.startsWith("PLATFORM")) {
            return AuditResourceType.PLATFORM_SETTINGS;
        }
        if (action.startsWith("AUTH") || action.contains("LOGIN") || action.contains("LOGOUT")) {
            return AuditResourceType.AUTH;
        }
        return null;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
