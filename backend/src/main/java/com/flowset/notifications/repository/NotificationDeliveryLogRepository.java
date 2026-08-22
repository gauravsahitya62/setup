package com.flowset.notifications.repository;

import com.flowset.notifications.entity.NotificationDeliveryLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, UUID> {
}
