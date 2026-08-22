package com.flowset.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    void mapsFiltersToTypedEnums() {
        assertThat(NotificationType.typesFor(NotificationFilter.BOOKINGS))
                .contains(
                        NotificationType.BOOKING_REQUEST_CREATED,
                        NotificationType.NEW_BOOKING_REQUEST,
                        NotificationType.BOOKING_REQUEST_ACCEPTED)
                .doesNotContain(NotificationType.ORDER_CONFIRMED, NotificationType.LOW_INVENTORY);
        assertThat(NotificationType.BOOKING_REQUEST_ACCEPTED.preferenceGroup())
                .isEqualTo(NotificationPreferenceGroup.BOOKING_UPDATES);
        assertThat(NotificationType.LOW_INVENTORY.filterCategory()).isEqualTo(NotificationFilter.INVENTORY);
    }
}
