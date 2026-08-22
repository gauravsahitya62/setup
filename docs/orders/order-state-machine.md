# Order state machine

Implemented by `OrderStateMachine`. Arbitrary status writes are rejected with 409.

| From | To |
| --- | --- |
| PENDING | CONFIRMED, CANCELLED |
| CONFIRMED | DELIVERY_SCHEDULED, CANCELLED |
| DELIVERY_SCHEDULED | OUT_FOR_DELIVERY, CANCELLED |
| OUT_FOR_DELIVERY | DELIVERED |
| DELIVERED | EVENT_COMPLETED |
| EVENT_COMPLETED | PICKUP_SCHEDULED |
| PICKUP_SCHEDULED | PICKED_UP |
| PICKED_UP | COMPLETED |
| COMPLETED | — |
| CANCELLED | — |

Orders created from a confirmed booking start as **CONFIRMED**. `PENDING → CONFIRMED` exists for completeness.

## Cancellation

Allowed from `PENDING`, `CONFIRMED`, `DELIVERY_SCHEDULED` only.

- After `OUT_FOR_DELIVERY`: 409 `This order can no longer be cancelled because delivery is underway`
- After `COMPLETED`: 409 `Completed orders cannot be cancelled`

Cancel is transactional: order `CANCELLED`, delivery `CANCELLED` when still scheduled, inventory released, linked CONFIRMED booking cancelled, history + audit + notification.

A planner cancel of a CONFIRMED **booking** fails if the linked order is no longer cancellable.

## Who may change status

| Action | Role | Endpoint |
| --- | --- | --- |
| View | Planner (owner), Vendor (storefront), Admin | `GET /orders/{id}`, `GET /vendor/orders/{id}`, `GET /admin/orders/{id}` |
| Cancel | Planner (owner) | `POST /orders/{id}/cancel` |
| Schedule delivery | Vendor | `POST /vendor/orders/{id}/schedule-delivery` |
| Out for delivery | Vendor | `POST /vendor/orders/{id}/out-for-delivery` |
| Mark delivered | Vendor | `POST /vendor/orders/{id}/mark-delivered` |
| Event completed | Vendor | `POST /vendor/orders/{id}/mark-event-completed` |
| Schedule pickup | Vendor | `POST /vendor/orders/{id}/schedule-pickup` |
| Mark picked up (→ COMPLETED) | Vendor | `POST /vendor/orders/{id}/mark-picked-up` |

Identity is taken from the JWT / vendor storefront. Cross-owner GET returns **404**.
