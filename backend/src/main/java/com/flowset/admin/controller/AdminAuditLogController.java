package com.flowset.admin.controller;

import com.flowset.admin.dto.AuditLogResponse;
import com.flowset.admin.service.AdminAuditQueryService;
import com.flowset.common.AuditResourceType;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Admin Audit Logs", description = "ADMIN authorization required. Immutable. No update or delete endpoints.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditQueryService adminAuditQueryService;

    public AdminAuditLogController(AdminAuditQueryService adminAuditQueryService) {
        this.adminAuditQueryService = adminAuditQueryService;
    }

    @GetMapping
    @Operation(summary = "Query audit logs")
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditResourceType resourceType,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminAuditQueryService.list(action, resourceType, actorUserId, resourceId, from, to, page, size);
    }
}
