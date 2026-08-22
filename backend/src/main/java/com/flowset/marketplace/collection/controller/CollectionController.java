package com.flowset.marketplace.collection.controller;

import com.flowset.marketplace.collection.dto.CollectionItemRequest;
import com.flowset.marketplace.collection.dto.CollectionItemResponse;
import com.flowset.marketplace.collection.dto.CollectionRequest;
import com.flowset.marketplace.collection.dto.CollectionResponse;
import com.flowset.marketplace.collection.dto.ItemReorderRequest;
import com.flowset.marketplace.collection.service.CollectionService;
import com.flowset.shared.dto.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collections")
@Tag(name = "Collections", description = "Vendor-curated product collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List published collections, optionally scoped to a vendor")
    public PageResponse<CollectionResponse> search(
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return collectionService.search(vendorId, page, size);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get a collection by id")
    public CollectionResponse getById(@PathVariable UUID id) {
        return collectionService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @SecurityRequirements
    @Operation(summary = "Get a collection by slug")
    public CollectionResponse getBySlug(@PathVariable String slug) {
        return collectionService.getBySlug(slug);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a collection for the vendor's own storefront")
    public ResponseEntity<CollectionResponse> create(@Valid @RequestBody CollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a collection")
    public CollectionResponse update(@PathVariable UUID id, @Valid @RequestBody CollectionRequest request) {
        return collectionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a collection")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        collectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/items")
    @SecurityRequirements
    @Operation(summary = "List products in a collection")
    public List<CollectionItemResponse> listItems(@PathVariable UUID id) {
        return collectionService.listItems(id);
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a product to a collection")
    public ResponseEntity<CollectionItemResponse> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.addItem(id, request));
    }

    @DeleteMapping("/{id}/items/{productId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove a product from a collection")
    public ResponseEntity<Void> removeItem(@PathVariable UUID id, @PathVariable UUID productId) {
        collectionService.removeItem(id, productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/items/reorder")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reorder products within a collection")
    public List<CollectionItemResponse> reorderItems(@PathVariable UUID id, @Valid @RequestBody ItemReorderRequest request) {
        return collectionService.reorderItems(id, request.productIds());
    }
}
