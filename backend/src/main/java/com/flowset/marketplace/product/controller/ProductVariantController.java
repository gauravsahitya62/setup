package com.flowset.marketplace.product.controller;

import com.flowset.marketplace.product.dto.VariantRequest;
import com.flowset.marketplace.product.dto.VariantResponse;
import com.flowset.marketplace.product.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/variants")
@Tag(name = "Product Variants", description = "Rentable variants (SKUs) of a product")
public class ProductVariantController {

    private final ProductVariantService variantService;

    public ProductVariantController(ProductVariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List variants of a product")
    public List<VariantResponse> list(@PathVariable UUID productId) {
        return variantService.list(productId);
    }

    @GetMapping("/{variantId}")
    @SecurityRequirements
    @Operation(summary = "Get a variant")
    public VariantResponse getById(@PathVariable UUID productId, @PathVariable UUID variantId) {
        return variantService.getById(productId, variantId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a variant (auto-creates matching inventory)")
    public ResponseEntity<VariantResponse> create(
            @PathVariable UUID productId,
            @Valid @RequestBody VariantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.create(productId, request));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a variant")
    public VariantResponse update(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantRequest request
    ) {
        return variantService.update(productId, variantId, request);
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a variant")
    public ResponseEntity<Void> delete(@PathVariable UUID productId, @PathVariable UUID variantId) {
        variantService.delete(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
