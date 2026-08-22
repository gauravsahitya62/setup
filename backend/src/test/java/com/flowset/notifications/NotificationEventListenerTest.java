package com.flowset.notifications;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.flowset.events.BookingRequestAcceptedEvent;
import com.flowset.notifications.dto.CreateNotificationCommand;
import com.flowset.notifications.listener.NotificationEventListener;
import com.flowset.notifications.service.NotificationService;
import com.flowset.users.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationEventListenerTest {

    @Test
    void listenerSwallowsNotificationFailuresSoBusinessEventsStaySuccessful() {
        NotificationService notificationService = mock(NotificationService.class);
        doThrow(new RuntimeException("delivery failed"))
                .when(notificationService)
                .createNotification(any(CreateNotificationCommand.class));
        NotificationEventListener listener =
                new NotificationEventListener(notificationService, mock(UserRepository.class));

        BookingRequestAcceptedEvent event = new BookingRequestAcceptedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sharma Wedding",
                "Royal Event Rentals");

        assertThatCode(() -> listener.onBookingAccepted(event)).doesNotThrowAnyException();
        verify(notificationService).createNotification(any(CreateNotificationCommand.class));
    }
}
