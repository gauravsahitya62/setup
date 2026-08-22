package com.flowset.notifications.repository;

import com.flowset.common.NotificationType;
import com.flowset.notifications.entity.NotificationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
        extends JpaRepository<NotificationEntity, UUID>, JpaSpecificationExecutor<NotificationEntity> {

    Optional<NotificationEntity> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    long countByRecipientUserIdAndReadFalse(UUID recipientUserId);

    boolean existsByRecipientUserIdAndTypeAndResourceIdAndReadFalse(
            UUID recipientUserId, NotificationType type, UUID resourceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationEntity n
            set n.read = true, n.readAt = :now
            where n.recipientUserId = :userId and n.read = false
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") java.time.Instant now);
}
