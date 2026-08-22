package com.flowset.marketplace.inventory.controller;

import com.flowset.marketplace.inventory.dto.AvailabilityCheckResponse;
import com.flowset.marketplace.inventory.service.InventoryAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/{variantId}/availability")
@SecurityRequirements
@Tag(name = "Availability", description = "Public date-window availability for a product variant")
public class PublicAvailabilityController {

    private final InventoryAvailabilityService availabilityService;

    public PublicAvailabilityController(InventoryAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(
            summary = "Check availability for a variant over an inclusive date range",
            description = "Rental dates are inclusive. PENDING reservations do not consume stock. "
                    + "Available quantity is calculated from total, standing maintenance/damaged, "
                    + "CONFIRMED reservations, and ACTIVE blocks."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Availability calculated",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "variantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                              "available": false,
                              "status": "PARTIAL",
                              "requestedQuantity": 40,
                              "availableQuantity": 30,
                              "message": "Only 30 available for these dates.",
                              "conflicts": [
                                {
                                  "date": "2026-12-12",
                                  "reservedQuantity": 70,
                                  "blockedQuantity": 0,
                                  "availableQuantity": 30,
                                  "type": "PARTIAL"
                                }
                              ],
                              "calendar": [
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
                            }
                            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date range or quantity",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "End date must be on or after start date"
                            }
                            """))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Variant inventory not found",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "status": 404,
                              "error": "Not Found",
                              "message": "Inventory not found for variant"
                            }
                            """)))
    })
    public AvailabilityCheckResponse check(
            @PathVariable UUID variantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer quantity
    ) {
        return availabilityService.checkAvailability(variantId, startDate, endDate, quantity);
    }
}
