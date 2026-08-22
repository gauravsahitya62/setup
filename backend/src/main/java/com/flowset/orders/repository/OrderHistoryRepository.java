package com.flowset.orders.repository;

import com.flowset.orders.entity.OrderHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntity, UUID> {

    List<OrderHistoryEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
