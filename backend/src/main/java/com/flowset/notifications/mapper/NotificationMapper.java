package com.flowset.notifications.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.notifications.dto.NotificationPreferencesResponse;
import com.flowset.notifications.dto.NotificationResponse;
import com.flowset.notifications.entity.NotificationEntity;
import com.flowset.notifications.entity.NotificationPreferenceEntity;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public NotificationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getActionUrl(),
                parseMetadata(entity.getMetadata()),
                entity.isRead(),
                entity.getReadAt(),
                entity.getCreatedAt(),
                entity.getExpiresAt());
    }

    public NotificationPreferencesResponse toPreferences(NotificationPreferenceEntity entity) {
        return new NotificationPreferencesResponse(
                entity.isBookingUpdates(),
                entity.isOrderUpdates(),
                entity.isDeliveryUpdates(),
                entity.isInventoryAlerts(),
                entity.isSystemAnnouncements());
    }

    public String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
