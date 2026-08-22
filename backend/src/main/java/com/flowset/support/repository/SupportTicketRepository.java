package com.flowset.support.repository;

import com.flowset.common.SupportTicketStatus;
import com.flowset.support.entity.SupportTicketEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    Optional<SupportTicketEntity> findByIdAndCreatedBy(UUID id, UUID createdBy);

    long countByStatus(SupportTicketStatus status);

    long countByStatusIn(java.util.Collection<SupportTicketStatus> statuses);
}
