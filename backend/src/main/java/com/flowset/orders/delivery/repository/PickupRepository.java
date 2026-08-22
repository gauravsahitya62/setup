package com.flowset.orders.delivery.repository;

import com.flowset.orders.delivery.entity.PickupEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRepository extends JpaRepository<PickupEntity, UUID> {

    Optional<PickupEntity> findByOrderId(UUID orderId);

    List<PickupEntity> findByOrderIdIn(Collection<UUID> orderIds);
}
