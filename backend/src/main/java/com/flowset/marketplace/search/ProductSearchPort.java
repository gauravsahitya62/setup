package com.flowset.marketplace.search;

import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.search.dto.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Abstraction over the product search backend. {@link JpaProductSearchAdapter} implements this today using
 * Spring Data JPA Specifications; a future Elasticsearch-backed adapter can implement the same port without
 * any change to {@code MarketplaceSearchService} or the search controller.
 */
public interface ProductSearchPort {

    Page<ProductEntity> search(SearchCriteria criteria, Pageable pageable);
}
