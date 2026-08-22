package com.flowset.marketplace.inventory.repository;

import com.flowset.common.InventoryBlockStatus;
import com.flowset.marketplace.inventory.entity.InventoryBlockEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBlockRepository extends JpaRepository<InventoryBlockEntity, UUID> {

    @Query("""
            select b from InventoryBlockEntity b
            where b.variant.id = :variantId
              and b.status = :status
              and b.startDate <= :endDate
              and b.endDate >= :startDate
            """)
    List<InventoryBlockEntity> findOverlapping(
            @Param("variantId") UUID variantId,
            @Param("status") InventoryBlockStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select b from InventoryBlockEntity b
            where b.variant.id in :variantIds
              and b.status = com.flowset.common.InventoryBlockStatus.ACTIVE
              and b.startDate <= :endDate
              and b.endDate >= :startDate
            """)
    List<InventoryBlockEntity> findActiveOverlappingForVariants(
            @Param("variantIds") Collection<UUID> variantIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<InventoryBlockEntity> findByVariantIdAndStatusOrderByStartDateAsc(
            UUID variantId, InventoryBlockStatus status);
}
