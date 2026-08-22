# Notification architecture

In-app notifications are a personal inbox. They are not project activity and not the audit log.

```
Domain service  --publish-->  Spring ApplicationEvent  --AFTER_COMMIT-->  NotificationEventListener
                                                                      |
                                                                      v
                                                            NotificationService
                                                                      |
                                                          InAppNotificationChannel
                                                                      |
                                                              notifications table
```

Package: `com.flowset.notifications`. Domain events live in `com.flowset.events` so a future Kafka/RabbitMQ consumer can replace `NotificationEventListener` without changing booking or order services.

## Channels

`NotificationChannel` is the delivery SPI. MVP implements `InAppNotificationChannel` only (persist + `notification_delivery_log`). Do not add Email, SMS, WhatsApp, or Push beans yet. `EmailNotificationService` remains the auth-token placeholder and is not the notification center.

## Real-time

MVP is request/response. The frontend polls unread count about once a minute when a session is open. WebSockets are not required. Adding a broker later should publish the same `com.flowset.events.*` records.

## Retention

`flowset.notifications.retention-days` (default 90) and `archive-enabled` (default false) are configuration only. MVP does not auto-delete recent notifications.

## Indexes

`recipient_user_id`, `is_read`, `created_at`, `type`, `resource_type`, `resource_id`, plus inbox composites. List queries are paginated and sorted newest first.

Frontend: `services/notification-api.ts`, `components/notifications/notification-center.tsx`, inbox pages at `/planner/notifications`, `/vendor/notifications`, `/admin/notifications`. `/notifications` redirects by role. Deep links use `ROUTES` on the client and `NotificationActionUrls` on the server.
