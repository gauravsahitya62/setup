package com.flowset.marketplace.search.dto;

import com.flowset.common.SearchSort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SearchCriteria(
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
        SearchSort sort
) {
}
