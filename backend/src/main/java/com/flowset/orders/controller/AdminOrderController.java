package com.flowset.orders.controller;

import com.flowset.common.OrderStatus;
import com.flowset.orders.dto.OrderResponse;
import com.flowset.orders.dto.OrderSummaryResponse;
import com.flowset.orders.service.OrderService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin Orders", description = "Support view of all orders. plannerId/vendorId query params are filters, not trusted identity.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders")
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
    @Operation(summary = "Get any order")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }
}
