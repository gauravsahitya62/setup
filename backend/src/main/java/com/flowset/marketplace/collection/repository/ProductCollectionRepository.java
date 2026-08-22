package com.flowset.marketplace.collection.repository;

import com.flowset.marketplace.collection.entity.ProductCollectionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCollectionRepository extends JpaRepository<ProductCollectionEntity, UUID> {

    @Query("select c from ProductCollectionEntity c where lower(c.slug) = lower(:slug)")
    Optional<ProductCollectionEntity> findBySlugIgnoreCase(@Param("slug") String slug);

    boolean existsByVendorIdAndSlugIgnoreCase(UUID vendorId, String slug);

    boolean existsByVendorIdAndSlugIgnoreCaseAndIdNot(UUID vendorId, String slug, UUID id);

    @Query("""
            select c from ProductCollectionEntity c
            where (:includeHidden = true or c.status = com.flowset.common.CollectionStatus.ACTIVE)
              and (:vendorId is null or c.vendor.id = :vendorId)
            """)
    Page<ProductCollectionEntity> search(
            @Param("vendorId") UUID vendorId,
            @Param("includeHidden") boolean includeHidden,
            Pageable pageable);
}
