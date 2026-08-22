package com.flowset.marketplace.inventory.controller;

import com.flowset.marketplace.inventory.dto.CalendarDayResponse;
import com.flowset.marketplace.inventory.dto.InventoryAdjustRequest;
import com.flowset.marketplace.inventory.dto.InventoryBlockRequest;
import com.flowset.marketplace.inventory.dto.InventoryBlockResponse;
import com.flowset.marketplace.inventory.dto.InventoryMovementResponse;
import com.flowset.marketplace.inventory.dto.InventoryResponse;
import com.flowset.marketplace.inventory.dto.InventoryUpdateRequest;
import com.flowset.marketplace.inventory.dto.MaintenanceCreateRequest;
import com.flowset.marketplace.inventory.dto.MaintenanceRecordResponse;
import com.flowset.marketplace.inventory.dto.VendorInventoryDetailResponse;
import com.flowset.marketplace.inventory.dto.VendorInventoryOverviewResponse;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
import com.flowset.marketplace.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendor")
@PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vendor inventory", description = "Owner-scoped stock, maintenance, movements, and calendar")
public class VendorInventoryController {

    private final InventoryService inventoryService;
    private final InventoryAvailabilityService availabilityService;

    public VendorInventoryController(
            InventoryService inventoryService,
            InventoryAvailabilityService availabilityService
    ) {
        this.inventoryService = inventoryService;
        this.availabilityService = availabilityService;
    }

    @GetMapping("/inventory")
    @Operation(summary = "List the authenticated vendor's inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory overview"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Planner or other vendor cannot list this inventory")
    })
    public VendorInventoryOverviewResponse list() {
        return inventoryService.overview();
    }

    @GetMapping("/inventory/{variantId}")
    @Operation(summary = "Get inventory detail for a variant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variant inventory"),
            @ApiResponse(responseCode = "403", description = "Not the owning vendor"),
            @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public VendorInventoryDetailResponse get(@PathVariable UUID variantId) {
        return inventoryService.getVendorInventory(variantId);
    }

    @PatchMapping("/inventory/{variantId}")
    @Operation(summary = "Update standing totals (total, maintenance, damaged)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "variantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                              "totalQuantity": 120,
                              "maintenance": 5,
                              "damaged": 2,
                              "available": 113
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Inconsistent quantities"),
            @ApiResponse(responseCode = "403", description = "Not the owning vendor"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    public InventoryResponse patch(
            @PathVariable UUID variantId,
            @Valid @RequestBody InventoryUpdateRequest request
    ) {
        inventoryService.assertOwnerOrAdmin(variantId);
        return inventoryService.patchInventory(variantId, request);
    }

    @PostMapping("/inventory/{variantId}/adjust")
    @Operation(summary = "Adjust total quantity by a signed delta. Reason is required.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Adjusted",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "totalQuantity": 120,
                              "available": 120
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Missing reason, zero delta, or negative result"),
            @ApiResponse(responseCode = "403", description = "Not the owning vendor")
    })
    public InventoryResponse adjust(
            @PathVariable UUID variantId,
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        inventoryService.assertOwnerOrAdmin(variantId);
        return inventoryService.adjustInventory(variantId, request);
    }

    @PostMapping("/inventory/{variantId}/maintenance")
    @Operation(summary = "Create a date-range maintenance block")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Maintenance created"),
            @ApiResponse(responseCode = "400", description = "Invalid dates or quantity"),
            @ApiResponse(responseCode = "403", description = "Not the owning vendor"),
            @ApiResponse(responseCode = "409", description = "Would oversell remaining availability")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordResponse createMaintenance(
            @PathVariable UUID variantId,
            @Valid @RequestBody MaintenanceCreateRequest request
    ) {
        inventoryService.assertOwnerOrAdmin(variantId);
        return availabilityService.createMaintenance(variantId, request);
    }

    @DeleteMapping("/inventory/{variantId}/maintenance/{id}")
    @Operation(summary = "Remove a maintenance block and restore availability")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "403", description = "Not the owning vendor"),
            @ApiResponse(responseCode = "404", description = "Maintenance record not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaintenance(@PathVariable UUID variantId, @PathVariable UUID id) {
        inventoryService.assertOwnerOrAdmin(variantId);
        availabilityService.deleteMaintenance(variantId, id);
    }

    @GetMapping("/inventory/{variantId}/movements")
    @Operation(summary = "List inventory movement history")
    @ApiResponse(responseCode = "200", description = "Movements newest first")
    public List<InventoryMovementResponse> movements(
            @PathVariable UUID variantId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return inventoryService.listMovements(variantId, limit);
    }

    @PostMapping("/inventory/{variantId}/blocks")
    @Operation(summary = "Manually block quantity for a date range")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryBlockResponse block(
            @PathVariable UUID variantId,
            @Valid @RequestBody InventoryBlockRequest request
    ) {
        inventoryService.assertOwnerOrAdmin(variantId);
        return availabilityService.blockInventory(variantId, request);
    }

    @DeleteMapping("/inventory/{variantId}/blocks/{blockId}")
    @Operation(summary = "Release a manual inventory block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable UUID variantId, @PathVariable UUID blockId) {
        inventoryService.assertOwnerOrAdmin(variantId);
        availabilityService.unblockInventory(blockId);
    }

    @GetMapping("/calendar")
    @Operation(summary = "Vendor availability calendar for a date window")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "One row per date (aggregated unless a single variant is selected)",
                    content = @Content(examples = @ExampleObject(value = """
                            [
                              {
                                "date": "2026-12-12",
                                "totalQuantity": 100,
                                "reservedQuantity": 70,
                                "maintenanceQuantity": 0,
                                "damagedQuantity": 0,
                                "blockedQuantity": 0,
                                "availableQuantity": 30
                              }
                            ]
                            """))),
            @ApiResponse(responseCode = "400", description = "Invalid or oversized date range"),
            @ApiResponse(responseCode = "403", description = "Not a vendor or admin")
    })
    public List<CalendarDayResponse> calendar(
            @RequestParam(required = false) UUID variant,
            @RequestParam(required = false) UUID product,
            @RequestParam(required = false) UUID category,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return inventoryService.vendorCalendar(variant, product, category, startDate, endDate);
    }
}
