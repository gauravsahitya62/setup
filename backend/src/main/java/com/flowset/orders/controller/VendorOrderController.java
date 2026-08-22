package com.flowset.orders.controller;

import com.flowset.common.OrderStatus;
import com.flowset.orders.dto.OrderResponse;
import com.flowset.orders.dto.OrderSummaryResponse;
import com.flowset.orders.dto.ScheduleDeliveryRequest;
import com.flowset.orders.dto.SchedulePickupRequest;
import com.flowset.orders.dto.VendorDeliveryCalendarResponse;
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
import java.time.LocalDate;
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
@RequestMapping("/api/v1/vendor/orders")
@Tag(
        name = "Vendor Orders",
        description = "Vendor fulfillment. vendorId is taken from the authenticated owner — never from the client."
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('VENDOR')")
public class VendorOrderController {

    private final OrderService orderService;

    public VendorOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List this vendor's orders")
    public PageResponse<OrderSummaryResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.list(projectId, status, null, null, page, size);
    }

    @GetMapping("/calendar")
    @Operation(
            summary = "Delivery calendar",
            description = "Today's deliveries, upcoming deliveries, pending pickups, and completed deliveries. Filter by date, order status, and city."
    )
    public VendorDeliveryCalendarResponse calendar(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String city
    ) {
        return orderService.calendar(date, status, city);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order belonging to this vendor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order"),
            @ApiResponse(responseCode = "404", description = "Not found (also used to hide other vendors' orders)")
    })
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }

    @PostMapping("/{id}/schedule-delivery")
    @Operation(
            summary = "Schedule delivery",
            description = "CONFIRMED → DELIVERY_SCHEDULED. Venue and contact are required (copied from the booking request when omitted)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled"),
            @ApiResponse(responseCode = "400", description = "Missing venue/contact or invalid date"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public OrderResponse scheduleDelivery(@PathVariable UUID id, @Valid @RequestBody ScheduleDeliveryRequest request) {
        return orderService.scheduleDelivery(id, request);
    }

    @PostMapping("/{id}/out-for-delivery")
    @Operation(summary = "Mark out for delivery", description = "DELIVERY_SCHEDULED → OUT_FOR_DELIVERY")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Out for delivery"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public OrderResponse outForDelivery(@PathVariable UUID id) {
        return orderService.markOutForDelivery(id);
    }

    @PostMapping("/{id}/mark-delivered")
    @Operation(summary = "Mark delivered", description = "OUT_FOR_DELIVERY → DELIVERED")
    public OrderResponse markDelivered(@PathVariable UUID id) {
        return orderService.markDelivered(id);
    }

    @PostMapping("/{id}/mark-event-completed")
    @Operation(summary = "Mark event completed", description = "DELIVERED → EVENT_COMPLETED")
    public OrderResponse markEventCompleted(@PathVariable UUID id) {
        return orderService.markEventCompleted(id);
    }

    @PostMapping("/{id}/schedule-pickup")
    @Operation(
            summary = "Schedule pickup",
            description = "EVENT_COMPLETED → PICKUP_SCHEDULED. Pickup date cannot be before delivery."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Pickup before delivery",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 400, "error": "Bad Request", "message": "Pickup cannot be scheduled before delivery" }
                            """)))
    })
    public OrderResponse schedulePickup(@PathVariable UUID id, @Valid @RequestBody SchedulePickupRequest request) {
        return orderService.schedulePickup(id, request);
    }

    @PostMapping("/{id}/mark-picked-up")
    @Operation(
            summary = "Mark picked up and complete the order",
            description = "PICKUP_SCHEDULED → PICKED_UP → COMPLETED in one transaction. Releases inventory via InventoryAvailabilityService."
    )
    public OrderResponse markPickedUp(@PathVariable UUID id) {
        return orderService.markPickedUp(id);
    }
}
