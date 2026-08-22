package com.flowset.orders.repository;

import com.flowset.orders.entity.OrderItemEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    List<OrderItemEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    List<OrderItemEntity> findByOrderIdIn(Collection<UUID> orderIds);

    List<OrderItemEntity> findByProductVariantIdIn(Collection<UUID> variantIds);
}
