package com.flowset.marketplace.product.controller;

import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.service.ProductService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/products")
@Tag(name = "Products", description = "Products scoped to a single vendor storefront")
public class VendorProductController {

    private final ProductService productService;

    public VendorProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List a vendor's products (published only for public callers)")
    public PageResponse<ProductSummaryResponse> list(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return productService.listByVendor(vendorId, page, size);
    }
}
