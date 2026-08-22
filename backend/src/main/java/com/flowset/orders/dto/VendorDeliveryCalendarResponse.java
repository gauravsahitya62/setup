package com.flowset.orders.dto;

import com.flowset.common.DeliveryStatus;
import com.flowset.common.OrderStatus;
import com.flowset.common.PickupStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record VendorDeliveryCalendarResponse(
        LocalDate date,
        List<CalendarEntry> todayDeliveries,
        List<CalendarEntry> upcomingDeliveries,
        List<CalendarEntry> pendingPickups,
        List<CalendarEntry> completedDeliveries
) {
    public record CalendarEntry(
            UUID orderId,
            String orderNumber,
            OrderStatus orderStatus,
            String kind,
            LocalDate date,
            LocalTime preferredTime,
            String city,
            String venueName,
            String plannerName,
            String projectName,
            DeliveryStatus deliveryStatus,
            PickupStatus pickupStatus,
            BigDecimal totalAmount,
            String currency
    ) {
    }
}
