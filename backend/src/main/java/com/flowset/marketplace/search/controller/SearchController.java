package com.flowset.marketplace.search.controller;

import com.flowset.common.SearchSort;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.search.service.MarketplaceSearchService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Marketplace product search")
public class SearchController {

    private final MarketplaceSearchService searchService;

    public SearchController(MarketplaceSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Search published products")
    public PageResponse<ProductSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID subcategoryId,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableTo,
            @RequestParam(required = false) Boolean verifiedVendor,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false, defaultValue = "NEWEST") SearchSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return searchService.search(
                keyword,
                categoryId,
                subcategoryId,
                vendorId,
                city,
                minPrice,
                maxPrice,
                availableFrom,
                availableTo,
                verifiedVendor,
                minRating,
                sort,
                page,
                size);
    }
}
