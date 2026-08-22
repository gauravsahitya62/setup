package com.flowset.notifications.channel;

import com.flowset.common.NotificationChannelType;
import com.flowset.common.NotificationDeliveryStatus;
import com.flowset.notifications.entity.NotificationDeliveryLogEntity;
import com.flowset.notifications.entity.NotificationEntity;
import com.flowset.notifications.repository.NotificationDeliveryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationChannel.class);

    private final NotificationDeliveryLogRepository deliveryLogRepository;

    public InAppNotificationChannel(NotificationDeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public void deliver(NotificationEntity notification) {
        NotificationDeliveryLogEntity row = new NotificationDeliveryLogEntity();
        row.setNotification(notification);
        row.setChannel(NotificationChannelType.IN_APP);
        row.setStatus(NotificationDeliveryStatus.SENT);
        deliveryLogRepository.save(row);
        log.debug("IN_APP notification delivered id={} type={}", notification.getId(), notification.getType());
    }
}
