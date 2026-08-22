package com.flowset.support.repository;

import com.flowset.support.entity.SupportMessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, UUID> {

    List<SupportMessageEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
