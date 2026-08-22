package com.flowset.admin.controller;

import com.flowset.admin.dto.AdminUserResponse;
import com.flowset.admin.dto.AuditLogResponse;
import com.flowset.admin.dto.ReasonRequest;
import com.flowset.admin.service.AdminUserService;
import com.flowset.auth.dto.MessageResponse;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "ADMIN authorization required. Cannot set passwords or impersonate users.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List users")
    public PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Boolean pendingVerification,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.list(q, role, status, pendingVerification, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user")
    public AdminUserResponse get(@PathVariable UUID id) {
        return adminUserService.get(id);
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend user")
    public AdminUserResponse suspend(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return adminUserService.suspend(id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate user")
    public AdminUserResponse reactivate(@PathVariable UUID id) {
        return adminUserService.reactivate(id);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Send password reset email via the existing reset flow")
    public MessageResponse resetPassword(@PathVariable UUID id) {
        return adminUserService.requestPasswordReset(id);
    }

    @GetMapping("/{id}/activity")
    @Operation(summary = "User audit activity")
    public PageResponse<AuditLogResponse> activity(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.activity(id, page, size);
    }
}
