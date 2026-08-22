# Delivery workflow

Vendor is responsible for delivering rental goods to the event venue. Planner can view status. Admin can view and update records for support. Planner **cannot** change delivery status.

```
Schedule delivery (order CONFIRMED → DELIVERY_SCHEDULED)
  → Delivery entity SCHEDULED
  → Out for delivery (order + delivery OUT_FOR_DELIVERY)
  → Delivered (order + delivery DELIVERED, actual_delivery_time set)
  → optional proof of delivery (photo / PDF)
  → Event completed
  → Schedule pickup (cannot be before delivery date)
  → Picked up → order COMPLETED
```

## Delivery statuses

`SCHEDULED` → `OUT_FOR_DELIVERY` | `CANCELLED`

`OUT_FOR_DELIVERY` → `DELIVERED` | `FAILED`

`FAILED` → `SCHEDULED` | `CANCELLED` (retry / abort)

`DELIVERED` and `CANCELLED` are terminal for the successful/cancel paths.

History is stored on `delivery_status_history` (previous status, new status, changed by, timestamp, notes).

## Proof of delivery

`POST /api/v1/orders/{orderId}/delivery/proof` (multipart `file`). Allowed: JPEG, PNG, WebP, GIF, PDF. Max 10 MB. Stored via `StorageService` under `orders/{orderId}/delivery-proof`. Metadata only in `delivery_proofs`.

Allowed after the order is `OUT_FOR_DELIVERY`, `DELIVERED`, or `EVENT_COMPLETED`.

## Calendar

`GET /api/v1/vendor/orders/calendar?date=&status=&city=` returns today's deliveries, upcoming deliveries, pending pickups, and completed deliveries for the authenticated vendor.

## HTTP

| Method | Path | Auth |
| --- | --- | --- |
| GET | `/orders/{orderId}/delivery` | Planner, Vendor, Admin (owner-scoped) |
| PUT | `/orders/{orderId}/delivery` | Vendor, Admin — upserts details without advancing order status |
| GET | `/orders/{orderId}/delivery/history` | Planner, Vendor, Admin |
| POST | `/orders/{orderId}/delivery/proof` | Vendor, Admin |
