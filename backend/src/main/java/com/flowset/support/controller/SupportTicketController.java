package com.flowset.support.controller;

import com.flowset.shared.dto.PageResponse;
import com.flowset.support.dto.CreateSupportTicketRequest;
import com.flowset.support.dto.SupportCategoryResponse;
import com.flowset.support.dto.SupportMessageRequest;
import com.flowset.support.dto.SupportTicketResponse;
import com.flowset.support.dto.SupportTicketSummaryResponse;
import com.flowset.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support")
@Tag(name = "Support", description = "Planner and vendor support tickets. Callers only see their own tickets.")
@SecurityRequirement(name = "bearerAuth")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/categories")
    @Operation(summary = "List support categories")
    public List<SupportCategoryResponse> categories() {
        return supportTicketService.listCategories();
    }

    @PostMapping("/tickets")
    @Operation(summary = "Create a support ticket")
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody CreateSupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.create(request));
    }

    @GetMapping("/tickets")
    @Operation(summary = "List my support tickets")
    public PageResponse<SupportTicketSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return supportTicketService.listMine(page, size);
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Get my support ticket")
    public SupportTicketResponse get(@PathVariable UUID id) {
        return supportTicketService.getById(id);
    }

    @PostMapping("/tickets/{id}/messages")
    @Operation(summary = "Add a message to my ticket")
    public SupportTicketResponse message(@PathVariable UUID id, @Valid @RequestBody SupportMessageRequest request) {
        return supportTicketService.addMessage(id, request);
    }

    @PatchMapping("/tickets/{id}/close")
    @Operation(summary = "Close my ticket")
    public SupportTicketResponse close(@PathVariable UUID id) {
        return supportTicketService.close(id);
    }
}
