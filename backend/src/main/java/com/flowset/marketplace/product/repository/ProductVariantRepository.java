package com.flowset.marketplace.product.repository;

import com.flowset.marketplace.product.entity.ProductVariantEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    List<ProductVariantEntity> findByProductIdOrderByCreatedAtAsc(UUID productId);

    Optional<ProductVariantEntity> findByIdAndProductId(UUID id, UUID productId);

    @Query("""
            select v from ProductVariantEntity v
            join fetch v.product p
            join fetch p.vendor
            where v.id = :id
            """)
    Optional<ProductVariantEntity> findWithProductAndVendorById(@Param("id") UUID id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    long countByProductId(UUID productId);
}
