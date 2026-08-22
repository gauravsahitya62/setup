package com.flowset.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum NotificationType {
    BOOKING_REQUEST_CREATED(NotificationPreferenceGroup.BOOKING_UPDATES, NotificationFilter.BOOKINGS),
    NEW_BOOKING_REQUEST(NotificationPreferenceGroup.BOOKING_UPDATES, NotificationFilter.BOOKINGS),
    BOOKING_REQUEST_ACCEPTED(NotificationPreferenceGroup.BOOKING_UPDATES, NotificationFilter.BOOKINGS),
    BOOKING_REQUEST_REJECTED(NotificationPreferenceGroup.BOOKING_UPDATES, NotificationFilter.BOOKINGS),
    BOOKING_REQUEST_CANCELLED(NotificationPreferenceGroup.BOOKING_UPDATES, NotificationFilter.BOOKINGS),
    INVENTORY_UNAVAILABLE(NotificationPreferenceGroup.INVENTORY_ALERTS, NotificationFilter.INVENTORY),
    ORDER_CONFIRMED(NotificationPreferenceGroup.ORDER_UPDATES, NotificationFilter.ORDERS),
    DELIVERY_SCHEDULED(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    OUT_FOR_DELIVERY(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    ORDER_DELIVERED(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    PICKUP_SCHEDULED(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    ORDER_COMPLETED(NotificationPreferenceGroup.ORDER_UPDATES, NotificationFilter.ORDERS),
    UPCOMING_DELIVERY(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    DELIVERY_REMINDER(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    PICKUP_REMINDER(NotificationPreferenceGroup.DELIVERY_UPDATES, NotificationFilter.DELIVERY),
    LOW_INVENTORY(NotificationPreferenceGroup.INVENTORY_ALERTS, NotificationFilter.INVENTORY),
    VENDOR_VERIFIED(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    REVIEW_REMINDER(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    REVIEW_ELIGIBLE(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    REVIEW_RECEIVED(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    REVIEW_REPORTED(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    REVIEW_MODERATED(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    SYSTEM_ANNOUNCEMENT(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    NEW_VENDOR_REGISTRATION(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    VENDOR_DOCUMENT_SUBMITTED(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    VENDOR_REPORT(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM),
    SYSTEM_ALERT(NotificationPreferenceGroup.SYSTEM_ANNOUNCEMENTS, NotificationFilter.SYSTEM);

    private final NotificationPreferenceGroup preferenceGroup;
    private final NotificationFilter filterCategory;

    NotificationType(NotificationPreferenceGroup preferenceGroup, NotificationFilter filterCategory) {
        this.preferenceGroup = preferenceGroup;
        this.filterCategory = filterCategory;
    }

    public NotificationPreferenceGroup preferenceGroup() {
        return preferenceGroup;
    }

    public NotificationFilter filterCategory() {
        return filterCategory;
    }

    public static Set<NotificationType> typesFor(NotificationFilter filter) {
        if (filter == null || filter == NotificationFilter.ALL || filter == NotificationFilter.UNREAD) {
            return EnumSet.allOf(NotificationType.class);
        }
        return Arrays.stream(values())
                .filter(type -> type.filterCategory == filter)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class)));
    }
}
