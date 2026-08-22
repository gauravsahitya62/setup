package com.flowset.orders.repository;

import com.flowset.orders.entity.OrderNoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNoteRepository extends JpaRepository<OrderNoteEntity, UUID> {

    List<OrderNoteEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
