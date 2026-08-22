package com.flowset.marketplace.search;

import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.marketplace.search.dto.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Default {@link ProductSearchPort} implementation backed by Spring Data JPA Specifications
 * ({@link ProductSpecifications}). Swap this bean for an Elasticsearch-backed adapter later without
 * changing {@code MarketplaceSearchService} or any controller.
 */
@Component
public class JpaProductSearchAdapter implements ProductSearchPort {

    private final ProductRepository productRepository;

    public JpaProductSearchAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<ProductEntity> search(SearchCriteria criteria, Pageable pageable) {
        Specification<ProductEntity> spec = ProductSpecifications.allOf(
                ProductSpecifications.notDeleted(),
                ProductSpecifications.isPublished(),
                ProductSpecifications.vendorNotDeleted(),
                ProductSpecifications.vendorNotSuspended(),
                ProductSpecifications.keyword(criteria.keyword()),
                ProductSpecifications.categoryId(criteria.categoryId()),
                ProductSpecifications.subcategoryId(criteria.subcategoryId()),
                ProductSpecifications.vendorId(criteria.vendorId()),
                ProductSpecifications.city(criteria.city()),
                ProductSpecifications.priceBetween(criteria.minPrice(), criteria.maxPrice()),
                ProductSpecifications.verifiedVendor(criteria.verifiedVendor()),
                ProductSpecifications.minRating(criteria.minRating()),
                ProductSpecifications.availableBetween(criteria.availableFrom(), criteria.availableTo()));

        return productRepository.findAll(spec, pageable);
    }
}
