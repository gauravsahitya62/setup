package com.flowset.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowset.common.NotificationResourceType;
import com.flowset.common.NotificationType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        NotificationResourceType resourceType,
        UUID resourceId,
        String actionUrl,
        Map<String, Object> metadata,
        @JsonProperty("isRead") boolean isRead,
        Instant readAt,
        Instant createdAt,
        Instant expiresAt
) {
}
