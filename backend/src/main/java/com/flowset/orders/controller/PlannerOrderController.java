package com.flowset.orders.controller;

import com.flowset.common.OrderStatus;
import com.flowset.orders.dto.CreateOrderNoteRequest;
import com.flowset.orders.dto.OrderNoteResponse;
import com.flowset.orders.dto.OrderResponse;
import com.flowset.orders.dto.OrderSummaryResponse;
import com.flowset.orders.service.OrderService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/orders")
@Tag(
        name = "Orders",
        description = """
                Planner orders created from confirmed booking requests.
                Pricing is a snapshot (price/day × quantity × inclusive rental days). Delivery fee is optional.
                Security deposit is informational and is not included in totalAmount. Payment is not collected.
                Status: PENDING→CONFIRMED→DELIVERY_SCHEDULED→OUT_FOR_DELIVERY→DELIVERED→EVENT_COMPLETED→PICKUP_SCHEDULED→PICKED_UP→COMPLETED.
                Cancellation is allowed from PENDING, CONFIRMED, or DELIVERY_SCHEDULED only.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class PlannerOrderController {

    private final OrderService orderService;

    public PlannerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List orders", description = "Planners see their own orders. Admins may filter by plannerId or vendorId.")
    public PageResponse<OrderSummaryResponse> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID plannerId,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.list(projectId, status, plannerId, vendorId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "Get an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order"),
            @ApiResponse(responseCode = "404", description = "Not found (also used to hide other planners' orders)")
    })
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Cancel an order",
            description = "Allowed from PENDING, CONFIRMED, or DELIVERY_SCHEDULED. Releases inventory via InventoryAvailabilityService. Completed or in-transit orders cannot be cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Invalid status transition",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 409, "error": "Conflict", "message": "This order can no longer be cancelled because delivery is underway" }
                            """)))
    })
    public OrderResponse cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('PLANNER','VENDOR','ADMIN')")
    @Operation(summary = "Add an order note")
    public OrderNoteResponse addNote(@PathVariable UUID id, @Valid @RequestBody CreateOrderNoteRequest request) {
        return orderService.addNote(id, request);
    }
}
