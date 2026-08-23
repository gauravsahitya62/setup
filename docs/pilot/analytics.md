# MVP analytics definition

Instrument only the events in `lib/analytics.ts`. No email, phone, name, or tokens.

Enable the client dispatcher with `NEXT_PUBLIC_ENABLE_ANALYTICS=true`. Until a warehouse is connected, events are a `flowset:analytics` browser CustomEvent (and otherwise discarded).

## Events

| Event | When |
| --- | --- |
| `planner_registered` | Planner registration succeeds |
| `vendor_registered` | Vendor registration succeeds |
| `project_created` | Project create succeeds |
| `product_viewed` | Product detail mounts |
| `product_added_to_project` | Variant added to a project |
| `vendor_viewed` | Public vendor profile |
| `availability_checked` | Availability API returns |
| `booking_request_created` | Planner sends request |
| `booking_request_accepted` | Vendor accept succeeds |
| `booking_request_rejected` | Vendor reject succeeds |
| `order_created` | Accept returned an `orderId` |
| `delivery_completed` | Vendor marks delivered |
| `order_completed` | Vendor marks picked up / completed |
| `review_submitted` | First review create (not edit) |

## Product metrics (warehouse later)

See [`pilot-success-metrics.md`](./pilot-success-metrics.md). GBV is estimated rental value on confirmed orders, **not revenue**.
