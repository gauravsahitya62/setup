package com.flowset.notifications.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequest(
        @NotNull(message = "bookingUpdates is required") Boolean bookingUpdates,
        @NotNull(message = "orderUpdates is required") Boolean orderUpdates,
        @NotNull(message = "deliveryUpdates is required") Boolean deliveryUpdates,
        @NotNull(message = "inventoryAlerts is required") Boolean inventoryAlerts,
        @NotNull(message = "systemAnnouncements is required") Boolean systemAnnouncements
) {
}
