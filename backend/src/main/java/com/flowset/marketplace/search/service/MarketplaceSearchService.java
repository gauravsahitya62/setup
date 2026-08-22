package com.flowset.marketplace.search.service;

import com.flowset.common.SearchSort;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.mapper.ProductMapper;
import com.flowset.marketplace.search.ProductSearchPort;
import com.flowset.marketplace.search.dto.SearchCriteria;
import com.flowset.shared.dto.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceSearchService {

    private final ProductSearchPort productSearchPort;
    private final ProductMapper productMapper;

    public MarketplaceSearchService(ProductSearchPort productSearchPort, ProductMapper productMapper) {
        this.productSearchPort = productSearchPort;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> search(
            String keyword,
            UUID categoryId,
            UUID subcategoryId,
            UUID vendorId,
            String city,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate availableFrom,
            LocalDate availableTo,
            Boolean verifiedVendor,
            BigDecimal minRating,
            SearchSort sort,
            int page,
            int size
    ) {
        SearchCriteria criteria = new SearchCriteria(
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
                sort == null ? SearchSort.NEWEST : sort);

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 50), resolveSort(criteria.sort()));

        Page<ProductEntity> result = productSearchPort.search(criteria, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(productMapper::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private Sort resolveSort(SearchSort sort) {
        return switch (sort) {
            case NEWEST -> Sort.by("createdAt").descending();
            case PRICE_ASC -> Sort.by("pricePerDay").ascending();
            case PRICE_DESC -> Sort.by("pricePerDay").descending();
            case RATING -> Sort.by("vendor.rating").descending();
            case POPULARITY -> Sort.by("featured").descending().and(Sort.by("createdAt").descending());
        };
    }
}
