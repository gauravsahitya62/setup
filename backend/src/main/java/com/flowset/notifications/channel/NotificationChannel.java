package com.flowset.notifications.channel;

import com.flowset.common.NotificationChannelType;
import com.flowset.notifications.entity.NotificationEntity;

/**
 * Delivery channel for a persisted in-app notification.
 * MVP implements {@link InAppNotificationChannel} only. Future beans:
 * EmailNotificationChannel, SmsNotificationChannel, WhatsAppNotificationChannel,
 * PushNotificationChannel. Kafka/RabbitMQ can replace Spring events without
 * changing this interface.
 */
public interface NotificationChannel {

    NotificationChannelType type();

    void deliver(NotificationEntity notification);
}
