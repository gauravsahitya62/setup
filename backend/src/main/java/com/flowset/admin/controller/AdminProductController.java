package com.flowset.admin.controller;

import com.flowset.admin.dto.ReasonRequest;
import com.flowset.admin.service.AdminProductService;
import com.flowset.common.ProductStatus;
import com.flowset.marketplace.product.dto.ProductResponse;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.service.ProductService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin Products", description = "ADMIN authorization required. Product moderation. No bulk destructive actions.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final ProductService productService;

    public AdminProductController(AdminProductService adminProductService, ProductService productService) {
        this.adminProductService = adminProductService;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products for moderation")
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminProductService.list(q, status, vendorId, categoryId, featured, page, size);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve product (PUBLISHED)")
    public ProductResponse approve(@PathVariable UUID id) {
        return productService.moderate(id, ProductStatus.PUBLISHED, null, null);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject product")
    public ProductResponse reject(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return productService.moderate(id, ProductStatus.REJECTED, request.reason(), null);
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend product")
    public ProductResponse suspend(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest request) {
        return productService.moderate(id, ProductStatus.SUSPENDED, request == null ? null : request.reason(), null);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore product to PUBLISHED")
    public ProductResponse restore(@PathVariable UUID id) {
        return productService.moderate(id, ProductStatus.PUBLISHED, null, null);
    }

    @PostMapping("/{id}/feature")
    @Operation(summary = "Feature product")
    public ProductResponse feature(@PathVariable UUID id) {
        return productService.moderate(id, null, null, true);
    }

    @PostMapping("/{id}/unfeature")
    @Operation(summary = "Unfeature product")
    public ProductResponse unfeature(@PathVariable UUID id) {
        return productService.moderate(id, null, null, false);
    }
}
