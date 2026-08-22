package com.flowset.notifications.repository;

import com.flowset.common.NotificationType;
import com.flowset.notifications.entity.NotificationTemplateEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByType(NotificationType type);
}
