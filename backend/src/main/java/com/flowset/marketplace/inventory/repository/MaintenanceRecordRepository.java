package com.flowset.marketplace.inventory.repository;

import com.flowset.common.MaintenanceRecordStatus;
import com.flowset.marketplace.inventory.entity.MaintenanceRecordEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecordEntity, UUID> {

    List<MaintenanceRecordEntity> findByVariantIdOrderByStartDateDesc(UUID variantId);

    List<MaintenanceRecordEntity> findByVariantIdAndStatusOrderByStartDateDesc(
            UUID variantId, MaintenanceRecordStatus status);
}
