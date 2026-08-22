package com.flowset.notifications.dto;

public record NotificationPreferencesResponse(
        boolean bookingUpdates,
        boolean orderUpdates,
        boolean deliveryUpdates,
        boolean inventoryAlerts,
        boolean systemAnnouncements
) {
}
