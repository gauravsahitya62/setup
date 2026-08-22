-- In-app notifications, preferences, templates, and delivery log.
-- Notifications, project activity, and audit logs are three separate stores.

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY,
    recipient_user_id   UUID NOT NULL REFERENCES users (id),
    type                VARCHAR(60) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    message             TEXT NOT NULL,
    resource_type       VARCHAR(40),
    resource_id         UUID,
    action_url          VARCHAR(500),
    metadata            TEXT,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_is_read ON notifications (is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_type ON notifications (type);
CREATE INDEX idx_notifications_resource_type ON notifications (resource_type);
CREATE INDEX idx_notifications_resource_id ON notifications (resource_id);
CREATE INDEX idx_notifications_inbox ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (recipient_user_id, is_read, created_at DESC);

CREATE TABLE notification_preferences (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL UNIQUE REFERENCES users (id),
    booking_updates         BOOLEAN NOT NULL DEFAULT TRUE,
    order_updates           BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_updates        BOOLEAN NOT NULL DEFAULT TRUE,
    inventory_alerts        BOOLEAN NOT NULL DEFAULT TRUE,
    system_announcements    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notification_templates (
    id              UUID PRIMARY KEY,
    type            VARCHAR(60) NOT NULL UNIQUE,
    title_template  VARCHAR(255) NOT NULL,
    body_template   TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notification_delivery_log (
    id              UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    channel         VARCHAR(30) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_delivery_log_notification ON notification_delivery_log (notification_id);
CREATE INDEX idx_notification_delivery_log_created_at ON notification_delivery_log (created_at);

INSERT INTO notification_templates (id, type, title_template, body_template, created_at) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'BOOKING_REQUEST_CREATED',
        'Booking request sent',
        '{vendorName} received your booking request for {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000002', 'NEW_BOOKING_REQUEST',
        'New booking request',
        '{projectName} has requested {itemSummary}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000003', 'BOOKING_REQUEST_ACCEPTED',
        'Booking accepted',
        '{vendorName} accepted your booking request for {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000004', 'BOOKING_REQUEST_REJECTED',
        'Booking declined',
        '{vendorName} declined your booking request for {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000005', 'BOOKING_REQUEST_CANCELLED',
        'Booking cancelled',
        'The booking request for {projectName} was cancelled.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000006', 'INVENTORY_UNAVAILABLE',
        'Inventory unavailable',
        'Inventory is no longer available for the booking request on {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000007', 'ORDER_CONFIRMED',
        'Order confirmed',
        'Order {orderNumber} is confirmed for {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000008', 'DELIVERY_SCHEDULED',
        'Delivery scheduled',
        'Your order {orderNumber} is scheduled for delivery on {deliveryDate}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000009', 'OUT_FOR_DELIVERY',
        'Out for delivery',
        'Order {orderNumber} is out for delivery.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000a', 'ORDER_DELIVERED',
        'Order delivered',
        'Order {orderNumber} was delivered to the venue.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000b', 'PICKUP_SCHEDULED',
        'Pickup scheduled',
        'Pickup for order {orderNumber} is scheduled on {pickupDate}.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000c', 'ORDER_COMPLETED',
        'Order completed',
        'Order {orderNumber} is complete.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000d', 'UPCOMING_DELIVERY',
        'Upcoming delivery',
        'Delivery for {projectName} ({orderNumber}) is scheduled on {deliveryDate}.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000e', 'DELIVERY_REMINDER',
        'Delivery reminder',
        'Reminder: deliver order {orderNumber} on {deliveryDate}.',
        NOW()),
    ('a1000000-0000-0000-0000-00000000000f', 'PICKUP_REMINDER',
        'Pickup reminder',
        'Pickup for order {orderNumber} is scheduled on {pickupDate}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000010', 'LOW_INVENTORY',
        'Low inventory',
        '{productName} is running low ({availableQuantity} available).',
        NOW()),
    ('a1000000-0000-0000-0000-000000000011', 'VENDOR_VERIFIED',
        'Storefront verified',
        'Your FlowSet storefront is verified.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000012', 'REVIEW_REMINDER',
        'Review reminder',
        'Share a review for {projectName}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000013', 'SYSTEM_ANNOUNCEMENT',
        'Announcement',
        '{message}',
        NOW()),
    ('a1000000-0000-0000-0000-000000000014', 'NEW_VENDOR_REGISTRATION',
        'New vendor registration',
        '{vendorName} registered a storefront.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000015', 'VENDOR_DOCUMENT_SUBMITTED',
        'Vendor document submitted',
        '{vendorName} uploaded a {documentType} document.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000016', 'VENDOR_REPORT',
        'Vendor report',
        '{message}',
        NOW()),
    ('a1000000-0000-0000-0000-000000000017', 'SYSTEM_ALERT',
        'System alert',
        '{message}',
        NOW());
