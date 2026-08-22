package com.flowset.orders.delivery.repository;

import com.flowset.orders.delivery.entity.DeliveryEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {

    Optional<DeliveryEntity> findByOrderId(UUID orderId);

    List<DeliveryEntity> findByOrderIdIn(Collection<UUID> orderIds);
}
