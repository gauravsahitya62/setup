package com.flowset.marketplace.inventory.controller;

import com.flowset.marketplace.inventory.dto.InventoryResponse;
import com.flowset.marketplace.inventory.dto.InventoryUpdateRequest;
import com.flowset.marketplace.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/variants/{variantId}/inventory")
@PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory", description = "Stock levels for product variants")
public class VariantInventoryController {

    private final InventoryService inventoryService;

    public VariantInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Get inventory for a variant")
    public InventoryResponse get(@PathVariable UUID variantId) {
        return inventoryService.getInventory(variantId);
    }

    @PutMapping
    @Operation(summary = "Update inventory totals for a variant")
    public InventoryResponse update(@PathVariable UUID variantId, @Valid @RequestBody InventoryUpdateRequest request) {
        return inventoryService.updateInventory(variantId, request);
    }
}
