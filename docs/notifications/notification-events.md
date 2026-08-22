# Notification events

Business services publish Spring application events. `NotificationEventListener` creates in-app rows after commit (inventory-unavailable after rollback).

| Event | Recipients | Types |
| --- | --- | --- |
| `BookingRequestCreatedEvent` | Vendor owner, planner | `NEW_BOOKING_REQUEST`, `BOOKING_REQUEST_CREATED` |
| `BookingRequestAcceptedEvent` | Planner, vendor owner | `BOOKING_REQUEST_ACCEPTED` |
| `BookingRequestRejectedEvent` | Planner | `BOOKING_REQUEST_REJECTED` |
| `BookingRequestCancelledEvent` | Vendor owner, planner | `BOOKING_REQUEST_CANCELLED` |
| `InventoryUnavailableEvent` | Planner | `INVENTORY_UNAVAILABLE` |
| `OrderConfirmedEvent` | Planner, vendor owner | `ORDER_CONFIRMED` |
| `DeliveryScheduledEvent` | Planner, vendor owner | `DELIVERY_SCHEDULED`, `UPCOMING_DELIVERY` |
| `OutForDeliveryEvent` | Planner | `OUT_FOR_DELIVERY` |
| `OrderDeliveredEvent` | Planner | `ORDER_DELIVERED` |
| `PickupScheduledEvent` | Planner, vendor owner | `PICKUP_SCHEDULED`, `PICKUP_REMINDER` |
| `OrderCompletedEvent` | Planner, vendor owner | `ORDER_COMPLETED`, `REVIEW_ELIGIBLE` (planner) |
| `ReviewPublishedEvent` | Planner, vendor owner | `REVIEW_RECEIVED` |
| `ReviewReportedEvent` | Admins | `REVIEW_REPORTED` |
| `ReviewModeratedEvent` | Planner, vendor owner | `REVIEW_MODERATED` |
| `InventoryLowEvent` | Vendor owner | `LOW_INVENTORY` |
| `VendorRegisteredEvent` | Admins | `NEW_VENDOR_REGISTRATION` |
| `VendorDocumentSubmittedEvent` | Admins | `VENDOR_DOCUMENT_SUBMITTED` |
| `VendorVerifiedEvent` | Vendor owner | `VENDOR_VERIFIED` |

Types that exist for templates but have no scheduler yet: `DELIVERY_REMINDER`, `REVIEW_REMINDER`, `SYSTEM_ANNOUNCEMENT`, `VENDOR_REPORT`, `SYSTEM_ALERT`.

Vendor review notifications never include review body text.

Deep links:

| Audience | Resource | Path (`ROUTES`) |
| --- | --- | --- |
| Planner | Booking | `/planner/booking-requests/{id}` |
| Vendor | Booking | `/vendor/requests/{id}` |
| Planner | Order | `/planner/orders/{id}` |
| Vendor | Order | `/vendor/bookings/{id}` |
| Planner | Project | `/planner/projects/{id}` |
| Vendor | Inventory | `/vendor/inventory/{variantId}` |
| Admin | Vendors | `/admin/vendors` |
| Vendor | Profile | `/vendor/profile` |
| Planner | Reviews | `/planner/reviews` |
| Vendor | Reviews | `/vendor/reviews` |
| Admin | Reviews | `/admin/reviews` |
