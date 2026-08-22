package com.flowset.orders.delivery.repository;

import com.flowset.orders.delivery.entity.DeliveryProofEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProofEntity, UUID> {

    List<DeliveryProofEntity> findByDeliveryIdOrderByUploadedAtAsc(UUID deliveryId);
}
