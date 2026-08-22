package com.flowset.marketplace.inventory.controller;

import com.flowset.marketplace.inventory.dto.AvailabilityResponse;
import com.flowset.marketplace.inventory.dto.AvailabilityUpdateRequest;
import com.flowset.marketplace.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/variants/{variantId}/availability")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Availability", description = "Day-by-day availability calendar for product variants")
public class VariantAvailabilityController {

    private final InventoryService inventoryService;

    public VariantAvailabilityController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List availability calendar entries for a variant")
    public List<AvailabilityResponse> list(
            @PathVariable UUID variantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return inventoryService.listAvailability(variantId, from, to);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Upsert an availability calendar entry for a variant")
    public AvailabilityResponse upsert(
            @PathVariable UUID variantId,
            @Valid @RequestBody AvailabilityUpdateRequest request
    ) {
        return inventoryService.upsertAvailability(variantId, request);
    }
}
