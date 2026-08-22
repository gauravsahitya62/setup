package com.flowset.marketplace.product.controller;

import com.flowset.marketplace.product.dto.AttributeRequest;
import com.flowset.marketplace.product.dto.AttributeResponse;
import com.flowset.marketplace.product.dto.ImageReorderRequest;
import com.flowset.marketplace.product.dto.ImageResponse;
import com.flowset.marketplace.product.dto.ProductRequest;
import com.flowset.marketplace.product.dto.ProductResponse;
import com.flowset.marketplace.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Vendor rental product catalog")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a product for the vendor's own storefront")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get a product by id")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @SecurityRequirements
    @Operation(summary = "Get a product by slug")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a product (vendor owner or admin)")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft-delete a product (vendor owner or admin)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload a product image")
    public ImageResponse addImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String altText
    ) {
        return productService.addImage(id, file, altText);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a product image")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/images/reorder")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reorder product images")
    public List<ImageResponse> reorderImages(@PathVariable UUID id, @Valid @RequestBody ImageReorderRequest request) {
        return productService.reorderImages(id, request.imageIds());
    }

    @GetMapping("/{id}/attributes")
    @SecurityRequirements
    @Operation(summary = "List product attributes")
    public List<AttributeResponse> listAttributes(@PathVariable UUID id) {
        return productService.listAttributes(id);
    }

    @PostMapping("/{id}/attributes")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a product attribute")
    public ResponseEntity<AttributeResponse> addAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody AttributeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addAttribute(id, request));
    }

    @PutMapping("/{id}/attributes/{attributeId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a product attribute")
    public AttributeResponse updateAttribute(
            @PathVariable UUID id,
            @PathVariable UUID attributeId,
            @Valid @RequestBody AttributeRequest request
    ) {
        return productService.updateAttribute(id, attributeId, request);
    }

    @DeleteMapping("/{id}/attributes/{attributeId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a product attribute")
    public ResponseEntity<Void> deleteAttribute(@PathVariable UUID id, @PathVariable UUID attributeId) {
        productService.deleteAttribute(id, attributeId);
        return ResponseEntity.noContent().build();
    }
}
