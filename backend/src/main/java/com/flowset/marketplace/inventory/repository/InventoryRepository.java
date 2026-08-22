package com.flowset.marketplace.inventory.repository;

import com.flowset.marketplace.inventory.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    Optional<InventoryEntity> findByVariantId(UUID variantId);

    boolean existsByVariantId(UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryEntity i where i.variant.id = :variantId")
    Optional<InventoryEntity> lockByVariantId(@Param("variantId") UUID variantId);

    @Query("""
            select i from InventoryEntity i
            join fetch i.variant v
            join fetch v.product p
            join fetch p.vendor
            where p.deletedAt is null
              and (:vendorId is null or p.vendor.id = :vendorId)
              and (:productId is null or p.id = :productId)
              and (:categoryId is null or p.category.id = :categoryId
                   or (p.subcategory is not null and p.subcategory.id = :categoryId))
              and (:variantId is null or v.id = :variantId)
            order by p.name, v.variantName
            """)
    List<InventoryEntity> findForVendor(
            @Param("vendorId") UUID vendorId,
            @Param("productId") UUID productId,
            @Param("categoryId") UUID categoryId,
            @Param("variantId") UUID variantId);

    @Query("""
            select i from InventoryEntity i
            join fetch i.variant v
            join fetch v.product p
            where v.id = :variantId
            """)
    Optional<InventoryEntity> findDetailByVariantId(@Param("variantId") UUID variantId);
}
