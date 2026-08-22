package com.flowset.marketplace.favorite.controller;

import com.flowset.marketplace.favorite.service.FavoriteService;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favorites", description = "Planner saved products")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "List the current planner's favorite products")
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return favoriteService.list(page, size);
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Add a product to favorites")
    public ResponseEntity<Void> add(@PathVariable UUID productId) {
        favoriteService.add(productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from favorites")
    public ResponseEntity<Void> remove(@PathVariable UUID productId) {
        favoriteService.remove(productId);
        return ResponseEntity.noContent().build();
    }
}
