package com.flowset.notifications.listener;

import com.flowset.common.NotificationResourceType;
import com.flowset.common.NotificationType;
import com.flowset.common.RoleName;
import com.flowset.events.BookingRequestAcceptedEvent;
import com.flowset.events.BookingRequestCancelledEvent;
import com.flowset.events.BookingRequestCreatedEvent;
import com.flowset.events.BookingRequestRejectedEvent;
import com.flowset.events.DeliveryScheduledEvent;
import com.flowset.events.InventoryLowEvent;
import com.flowset.events.InventoryUnavailableEvent;
import com.flowset.events.OrderCompletedEvent;
import com.flowset.events.OrderConfirmedEvent;
import com.flowset.events.OrderDeliveredEvent;
import com.flowset.events.OutForDeliveryEvent;
import com.flowset.events.PickupScheduledEvent;
import com.flowset.events.ReviewModeratedEvent;
import com.flowset.events.ReviewPublishedEvent;
import com.flowset.events.ReviewReportedEvent;
import com.flowset.events.VendorDocumentSubmittedEvent;
import com.flowset.events.VendorRegisteredEvent;
import com.flowset.events.VendorVerifiedEvent;
import com.flowset.notifications.NotificationActionUrls;
import com.flowset.notifications.dto.CreateNotificationCommand;
import com.flowset.notifications.service.NotificationService;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumes domain events after the business transaction commits (or rolls back
 * for inventory-unavailable) and creates in-app notifications. Failures are
 * swallowed so they never affect the originating use case. This listener is the
 * seam for a future Kafka/RabbitMQ consumer.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM");

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationEventListener(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingRequestCreatedEvent event) {
        safe("BOOKING_REQUEST_CREATED", () -> {
            Map<String, Object> meta = meta(
                    "projectName", event.projectName(),
                    "vendorName", event.vendorName(),
                    "itemSummary", event.itemSummary());
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.NEW_BOOKING_REQUEST,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.vendorBookingRequest(event.bookingRequestId()),
                    meta);
            create(
                    event.plannerId(),
                    NotificationType.BOOKING_REQUEST_CREATED,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.plannerBookingRequest(event.bookingRequestId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingAccepted(BookingRequestAcceptedEvent event) {
        safe("BOOKING_REQUEST_ACCEPTED", () -> {
            Map<String, Object> meta = meta("projectName", event.projectName(), "vendorName", event.vendorName());
            create(
                    event.plannerId(),
                    NotificationType.BOOKING_REQUEST_ACCEPTED,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.plannerBookingRequest(event.bookingRequestId()),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.BOOKING_REQUEST_ACCEPTED,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.vendorBookingRequest(event.bookingRequestId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRejected(BookingRequestRejectedEvent event) {
        safe("BOOKING_REQUEST_REJECTED", () -> create(
                event.plannerId(),
                NotificationType.BOOKING_REQUEST_REJECTED,
                NotificationResourceType.BOOKING_REQUEST,
                event.bookingRequestId(),
                NotificationActionUrls.plannerBookingRequest(event.bookingRequestId()),
                meta("projectName", event.projectName(), "vendorName", event.vendorName())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingRequestCancelledEvent event) {
        safe("BOOKING_REQUEST_CANCELLED", () -> {
            Map<String, Object> meta = meta("projectName", event.projectName(), "vendorName", event.vendorName());
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.BOOKING_REQUEST_CANCELLED,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.vendorBookingRequest(event.bookingRequestId()),
                    meta);
            create(
                    event.plannerId(),
                    NotificationType.BOOKING_REQUEST_CANCELLED,
                    NotificationResourceType.BOOKING_REQUEST,
                    event.bookingRequestId(),
                    NotificationActionUrls.plannerBookingRequest(event.bookingRequestId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onInventoryUnavailable(InventoryUnavailableEvent event) {
        safe("INVENTORY_UNAVAILABLE", () -> create(
                event.plannerId(),
                NotificationType.INVENTORY_UNAVAILABLE,
                NotificationResourceType.BOOKING_REQUEST,
                event.bookingRequestId(),
                NotificationActionUrls.plannerBookingRequest(event.bookingRequestId()),
                meta("projectName", event.projectName(), "vendorName", event.vendorName())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        safe("ORDER_CONFIRMED", () -> {
            Map<String, Object> meta = meta(
                    "orderNumber", event.orderNumber(),
                    "projectName", event.projectName(),
                    "vendorName", event.vendorName());
            create(
                    event.plannerId(),
                    NotificationType.ORDER_CONFIRMED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.plannerOrder(event.orderId()),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.ORDER_CONFIRMED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.vendorOrder(event.orderId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryScheduled(DeliveryScheduledEvent event) {
        safe("DELIVERY_SCHEDULED", () -> {
            String date = event.deliveryDate() == null ? "" : DATE.format(event.deliveryDate());
            Map<String, Object> meta = meta(
                    "orderNumber", event.orderNumber(),
                    "projectName", event.projectName(),
                    "vendorName", event.vendorName(),
                    "deliveryDate", date);
            create(
                    event.plannerId(),
                    NotificationType.DELIVERY_SCHEDULED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.plannerOrder(event.orderId()),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.UPCOMING_DELIVERY,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.vendorOrder(event.orderId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutForDelivery(OutForDeliveryEvent event) {
        safe("OUT_FOR_DELIVERY", () -> create(
                event.plannerId(),
                NotificationType.OUT_FOR_DELIVERY,
                NotificationResourceType.ORDER,
                event.orderId(),
                NotificationActionUrls.plannerOrder(event.orderId()),
                meta("orderNumber", event.orderNumber(), "projectName", event.projectName())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderDelivered(OrderDeliveredEvent event) {
        safe("ORDER_DELIVERED", () -> create(
                event.plannerId(),
                NotificationType.ORDER_DELIVERED,
                NotificationResourceType.ORDER,
                event.orderId(),
                NotificationActionUrls.plannerOrder(event.orderId()),
                meta("orderNumber", event.orderNumber(), "projectName", event.projectName())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPickupScheduled(PickupScheduledEvent event) {
        safe("PICKUP_SCHEDULED", () -> {
            String date = event.pickupDate() == null ? "" : DATE.format(event.pickupDate());
            Map<String, Object> meta = meta(
                    "orderNumber", event.orderNumber(),
                    "projectName", event.projectName(),
                    "pickupDate", date);
            create(
                    event.plannerId(),
                    NotificationType.PICKUP_SCHEDULED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.plannerOrder(event.orderId()),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.PICKUP_REMINDER,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.vendorOrder(event.orderId()),
                    meta);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        safe("ORDER_COMPLETED", () -> {
            Map<String, Object> meta = meta("orderNumber", event.orderNumber(), "projectName", event.projectName());
            create(
                    event.plannerId(),
                    NotificationType.ORDER_COMPLETED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.plannerOrder(event.orderId()),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.ORDER_COMPLETED,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.vendorOrder(event.orderId()),
                    meta);
            create(
                    event.plannerId(),
                    NotificationType.REVIEW_ELIGIBLE,
                    NotificationResourceType.ORDER,
                    event.orderId(),
                    NotificationActionUrls.plannerOrderReview(event.orderId()),
                    meta("vendorName", event.vendorName(), "orderNumber", event.orderNumber()));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVendorRegistered(VendorRegisteredEvent event) {
        safe("NEW_VENDOR_REGISTRATION", () -> {
            Map<String, Object> meta = meta("vendorName", event.companyName());
            for (UserEntity admin : userRepository.findActiveByRole(RoleName.ADMIN)) {
                create(
                        admin.getId(),
                        NotificationType.NEW_VENDOR_REGISTRATION,
                        NotificationResourceType.VENDOR,
                        event.vendorId(),
                        NotificationActionUrls.adminVendors(),
                        meta);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVendorDocumentSubmitted(VendorDocumentSubmittedEvent event) {
        safe("VENDOR_DOCUMENT_SUBMITTED", () -> {
            Map<String, Object> meta = meta(
                    "vendorName", event.companyName(),
                    "documentType", event.documentType() == null ? "" : event.documentType().name());
            for (UserEntity admin : userRepository.findActiveByRole(RoleName.ADMIN)) {
                create(
                        admin.getId(),
                        NotificationType.VENDOR_DOCUMENT_SUBMITTED,
                        NotificationResourceType.VENDOR,
                        event.vendorId(),
                        NotificationActionUrls.adminVendors(),
                        meta);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVendorVerified(VendorVerifiedEvent event) {
        safe("VENDOR_VERIFIED", () -> create(
                event.ownerUserId(),
                NotificationType.VENDOR_VERIFIED,
                NotificationResourceType.VENDOR,
                event.vendorId(),
                NotificationActionUrls.vendorProfile(),
                meta("vendorName", event.companyName())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryLow(InventoryLowEvent event) {
        safe("LOW_INVENTORY", () -> create(
                event.vendorOwnerUserId(),
                NotificationType.LOW_INVENTORY,
                NotificationResourceType.INVENTORY,
                event.variantId(),
                NotificationActionUrls.vendorInventory(event.variantId()),
                meta("productName", event.productName(), "availableQuantity", event.availableQuantity())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewPublished(ReviewPublishedEvent event) {
        safe("REVIEW_RECEIVED", () -> {
            create(
                    event.plannerId(),
                    NotificationType.REVIEW_RECEIVED,
                    NotificationResourceType.REVIEW,
                    event.reviewId(),
                    NotificationActionUrls.plannerReviews(),
                    meta(
                            "title", "Review published",
                            "message", "Your review for " + event.vendorName() + " is now published.",
                            "vendorName", event.vendorName(),
                            "orderNumber", event.orderNumber()));
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.REVIEW_RECEIVED,
                    NotificationResourceType.REVIEW,
                    event.reviewId(),
                    NotificationActionUrls.vendorReviews(),
                    meta(
                            "title", "New verified review",
                            "message", "You received a new verified review.",
                            "vendorName", event.vendorName()));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewReported(ReviewReportedEvent event) {
        safe("REVIEW_REPORTED", () -> {
            Map<String, Object> meta = meta("reason", event.reason() == null ? "" : event.reason().name());
            for (UserEntity admin : userRepository.findActiveByRole(RoleName.ADMIN)) {
                create(
                        admin.getId(),
                        NotificationType.REVIEW_REPORTED,
                        NotificationResourceType.REVIEW,
                        event.reviewId(),
                        NotificationActionUrls.adminReviews(),
                        meta);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewModerated(ReviewModeratedEvent event) {
        safe("REVIEW_MODERATED", () -> {
            Map<String, Object> meta = meta(
                    "vendorName", event.vendorName(),
                    "action", event.action() == null ? "" : event.action().name().toLowerCase());
            create(
                    event.plannerId(),
                    NotificationType.REVIEW_MODERATED,
                    NotificationResourceType.REVIEW,
                    event.reviewId(),
                    NotificationActionUrls.plannerReviews(),
                    meta);
            create(
                    event.vendorOwnerUserId(),
                    NotificationType.REVIEW_MODERATED,
                    NotificationResourceType.REVIEW,
                    event.reviewId(),
                    NotificationActionUrls.vendorReviews(),
                    meta);
        });
    }

    private void create(
            UUID recipientUserId,
            NotificationType type,
            NotificationResourceType resourceType,
            UUID resourceId,
            String actionUrl,
            Map<String, Object> metadata
    ) {
        if (recipientUserId == null) {
            return;
        }
        notificationService.createNotification(new CreateNotificationCommand(
                recipientUserId, type, resourceType, resourceId, actionUrl, metadata));
    }

    private void safe(String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error("Notification listener failed for {}", label, ex);
        }
    }

    private static Map<String, Object> meta(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
