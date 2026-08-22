package com.flowset.notifications.dto;

import com.flowset.common.NotificationResourceType;
import com.flowset.common.NotificationType;
import java.util.Map;
import java.util.UUID;

public record CreateNotificationCommand(
        UUID recipientUserId,
        NotificationType type,
        NotificationResourceType resourceType,
        UUID resourceId,
        String actionUrl,
        Map<String, Object> metadata
) {
}
