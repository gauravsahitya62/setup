package com.flowset.marketplace.inventory.repository;

import com.flowset.marketplace.inventory.entity.InventoryMovementEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID> {

    List<InventoryMovementEntity> findByVariantIdOrderByCreatedAtDesc(UUID variantId);

    Page<InventoryMovementEntity> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
}
