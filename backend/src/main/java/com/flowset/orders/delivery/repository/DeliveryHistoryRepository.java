package com.flowset.orders.delivery.repository;

import com.flowset.orders.delivery.entity.DeliveryHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistoryEntity, UUID> {

    List<DeliveryHistoryEntity> findByDeliveryIdOrderByCreatedAtAsc(UUID deliveryId);
}
