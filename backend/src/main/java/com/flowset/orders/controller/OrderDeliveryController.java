package com.flowset.orders.controller;

import com.flowset.orders.dto.DeliveryHistoryResponse;
import com.flowset.orders.dto.DeliveryProofResponse;
import com.flowset.orders.dto.DeliveryResponse;
import com.flowset.orders.dto.DeliveryUpdateRequest;
import com.flowset.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/delivery")
@Tag(
        name = "Order Delivery",
        description = "Vendor-owned delivery records. Planners can view. Admins can update for support. Bytes are stored via StorageService, not PostgreSQL."
)
@SecurityRequirement(name = "bearerAuth")
public class OrderDeliveryController {

    private final OrderService orderService;

    public OrderDeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','VENDOR','ADMIN')")
    @Operation(summary = "Get delivery details")
    public DeliveryResponse get(@PathVariable UUID orderId) {
        return orderService.getDelivery(orderId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(
            summary = "Update delivery details",
            description = "Upserts venue/contact/date without advancing order status. Planners cannot modify delivery."
    )
    public DeliveryResponse update(@PathVariable UUID orderId, @Valid @RequestBody DeliveryUpdateRequest request) {
        return orderService.updateDelivery(orderId, request);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('PLANNER','VENDOR','ADMIN')")
    @Operation(summary = "Delivery status history")
    public List<DeliveryHistoryResponse> history(@PathVariable UUID orderId) {
        return orderService.deliveryHistory(orderId);
    }

    @PostMapping(value = "/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @Operation(summary = "Upload proof of delivery", description = "Photo, PDF, document, or signature image. Max 10 MB. Allowed: JPEG, PNG, WebP, GIF, PDF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded"),
            @ApiResponse(responseCode = "400", description = "Unsupported type or file too large"),
            @ApiResponse(responseCode = "409", description = "Order is not in a deliverable status")
    })
    public DeliveryProofResponse uploadProof(@PathVariable UUID orderId, @RequestPart("file") MultipartFile file) {
        return orderService.uploadProof(orderId, file);
    }
}
