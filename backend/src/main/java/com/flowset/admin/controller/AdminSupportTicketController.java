package com.flowset.admin.controller;

import com.flowset.common.SupportTicketPriority;
import com.flowset.common.SupportTicketStatus;
import com.flowset.shared.dto.PageResponse;
import com.flowset.support.dto.AdminSupportTicketUpdateRequest;
import com.flowset.support.dto.SupportMessageRequest;
import com.flowset.support.dto.SupportTicketResponse;
import com.flowset.support.dto.SupportTicketSummaryResponse;
import com.flowset.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/support/tickets")
@Tag(name = "Admin Support", description = "ADMIN authorization required. Assign, prioritize, reply, resolve, and close tickets.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    public AdminSupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping
    @Operation(summary = "List all support tickets")
    public PageResponse<SupportTicketSummaryResponse> list(
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketPriority priority,
            @RequestParam(required = false) UUID assignedAdminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return supportTicketService.adminList(status, priority, assignedAdminId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any support ticket")
    public SupportTicketResponse get(@PathVariable UUID id) {
        return supportTicketService.getById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Assign, change priority, or change status")
    public SupportTicketResponse update(
            @PathVariable UUID id, @Valid @RequestBody AdminSupportTicketUpdateRequest request) {
        return supportTicketService.adminUpdate(id, request);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to a ticket")
    public SupportTicketResponse message(@PathVariable UUID id, @Valid @RequestBody SupportMessageRequest request) {
        return supportTicketService.addMessage(id, request);
    }
}
