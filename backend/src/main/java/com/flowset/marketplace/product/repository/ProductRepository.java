package com.flowset.marketplace.product.repository;

import com.flowset.marketplace.product.entity.ProductEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

    @Query("select p from ProductEntity p where p.id = :id and p.deletedAt is null")
    Optional<ProductEntity> findActiveById(@Param("id") UUID id);

    @Query("select p from ProductEntity p where lower(p.slug) = lower(:slug) and p.deletedAt is null")
    Optional<ProductEntity> findFirstActiveBySlugIgnoreCase(@Param("slug") String slug);

    boolean existsByVendorIdAndSlugIgnoreCaseAndDeletedAtIsNull(UUID vendorId, String slug);

    boolean existsByVendorIdAndSlugIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID vendorId, String slug, UUID id);

    @Query("""
            select p from ProductEntity p
            where p.deletedAt is null
              and p.vendor.id = :vendorId
              and (:includeAll = true or p.status = com.flowset.common.ProductStatus.PUBLISHED)
            """)
    Page<ProductEntity> findByVendor(
            @Param("vendorId") UUID vendorId,
            @Param("includeAll") boolean includeAll,
            Pageable pageable);

    @Query("""
            select case when count(p) > 0 then true else false end
            from ProductEntity p
            left join p.subcategory sc
            where p.deletedAt is null and (p.category.id = :categoryId or sc.id = :subcategoryId)
            """)
    boolean existsByCategoryIdOrSubcategoryId(
            @Param("categoryId") UUID categoryId,
            @Param("subcategoryId") UUID subcategoryId);

    long countByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(com.flowset.common.ProductStatus status);

    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(java.time.Instant from);

    java.util.List<ProductEntity> findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(
            java.time.Instant from);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int incrementViewCount(@Param("id") UUID id);
}
