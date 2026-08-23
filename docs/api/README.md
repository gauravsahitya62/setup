# Vendor API

Base path: `/api/v1`

OpenAPI/Swagger: `/swagger-ui.html` (development only; disabled on staging/prod profiles). Send `X-Request-ID` to correlate client and API logs.

## Marketplace

The rental catalog (categories, products/variants, images, inventory, availability, favorites,
collections) and the `GET /search` endpoint are documented in
[`docs/marketplace/README.md`](../marketplace/README.md) and
[`docs/search/README.md`](../search/README.md). Date-window inventory (reservations, blocks,
vendor calendar, public availability check) is documented in
[`docs/inventory/README.md`](../inventory/README.md). On the frontend, `services/marketplace-api.ts`
wraps every one of those endpoints (mirroring the `vendor-api.ts` pattern below) and is exported
from `services/index.ts` as `marketplaceApi`. `mapProductSummaryToListing` /
`mapProductResponseToListing` convert backend DTOs into the `MarketplaceListing` shape consumed by
`components/marketplace/*` (search experience, product detail, category pages, planner
favorites, vendor catalog, vendor inventory, and the product creation wizard).

## Storefront

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/vendors` | VENDOR, ADMIN | Create storefront |
| GET | `/vendors` | Public | Search: companyName, city, state, businessType, verified, minRating, page, size |
| GET | `/vendors/me` | VENDOR, ADMIN | Owner storefront |
| GET | `/vendors/{id}` | Public* | Drafts only for owner/admin |
| GET | `/vendors/slug/{slug}` | Public* | Published (or owner) |
| PUT | `/vendors/{id}` | Owner or ADMIN | Update |
| DELETE | `/vendors/{id}` | Owner or ADMIN | Soft delete |
| POST | `/vendors/{id}/logo` | Owner or ADMIN | multipart `file` |
| POST | `/vendors/{id}/banner` | Owner or ADMIN | multipart `file` |

\* Unpublished profiles are hidden from planners.

## Portfolio

| Method | Path |
| --- | --- |
| POST | `/vendors/{id}/portfolio` (multipart) |
| PUT | `/vendors/{id}/portfolio/{itemId}` |
| DELETE | `/vendors/{id}/portfolio/{itemId}` |

## Service areas

| Method | Path |
| --- | --- |
| GET | `/vendors/{id}/service-areas` |
| POST | `/vendors/{id}/service-areas` |
| PUT | `/vendors/{id}/service-areas/{areaId}` |
| DELETE | `/vendors/{id}/service-areas/{areaId}` |

## Social links

| Method | Path |
| --- | --- |
| GET | `/vendors/{id}/social-links` |
| PUT | `/vendors/{id}/social-links` |

## Documents

| Method | Path |
| --- | --- |
| GET | `/vendors/{id}/documents` |
| POST | `/vendors/{id}/documents?documentType=` (multipart) |
| DELETE | `/vendors/{id}/documents/{documentId}` |

## Files

| Method | Path | Auth |
| --- | --- | --- |
| GET | `/files/**` | Public | Serve stored objects |

## Validation

- Company name required
- Slug unique (auto-generated from name when omitted)
- Website valid URL when provided
- Email / phone format validation
- GST optional

## Permissions

| Role | Capability |
| --- | --- |
| VENDOR (owner) | Full CRUD on own storefront |
| PLANNER | Read published profiles only |
| ADMIN | Read/write any vendor |

## Planner projects

Frontend client: `services/project-api.ts` (`projectApi`). Swagger tag **Projects**. Ownership is always taken from the JWT — never from a client `plannerId`.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/projects` | PLANNER | Create. Default status `DRAFT`. 400 validation. |
| GET | `/projects` | PLANNER, ADMIN | Search `q`, filter `status` / `eventType`, sort `eventDate` (default), pagination. Admin may pass `plannerId`. |
| GET | `/projects/{id}` | Authenticated | Owner or admin. **404** if missing or not owned (hides existence). |
| GET | `/projects/slug/{slug}` | PLANNER | Current planner's slug. |
| PUT | `/projects/{id}` | PLANNER (owner) | Partial-style update. |
| DELETE | `/projects/{id}` | PLANNER (owner) | Soft delete. |
| POST | `/projects/{id}/cover` | PLANNER | multipart `file`. |
| POST | `/projects/{projectId}/products` | PLANNER | `{ variantId, quantity, notes }`. Does **not** reserve inventory. 409 if already added. |
| GET | `/projects/{projectId}/products` | PLANNER, ADMIN | Snapshots + `currentPricePerDay`. |
| PATCH | `/projects/{projectId}/products/{id}` | PLANNER | Quantity / notes. |
| DELETE | `/projects/{projectId}/products/{id}` | PLANNER | Does not change inventory. |
| POST | `/projects/{projectId}/vendors` | PLANNER | `{ vendorId, notes }`. |
| GET | `/projects/{projectId}/vendors` | PLANNER, ADMIN | |
| DELETE | `/projects/{projectId}/vendors/{vendorId}` | PLANNER | Does not cancel bookings. |
| GET | `/projects/{projectId}/mood-board` | PLANNER, ADMIN | |
| POST | `/projects/{projectId}/mood-board/items` | PLANNER | multipart **or** JSON `{ imageUrl, caption, sourceUrl }`. |
| PATCH | `/projects/{projectId}/mood-board/items/{id}` | PLANNER | |
| PUT | `/projects/{projectId}/mood-board/reorder` | PLANNER | `{ itemIds }`. |
| DELETE | `/projects/{projectId}/mood-board/items/{id}` | PLANNER | |
| POST/GET/PATCH/DELETE | `/projects/{projectId}/notes` | PLANNER (GET also ADMIN) | Pin via `pinned`. |
| POST/GET/DELETE | `/projects/{projectId}/files` | PLANNER (GET also ADMIN) | multipart `file`. |
| GET | `/projects/{projectId}/activity` | PLANNER, ADMIN | Immutable timeline. |

Validation errors use the standard `{ status, error, message, fieldErrors }` envelope. Unauthorized → 401. Vendor mutating a project → 403. Cross-planner GET by id → 404.

See [`docs/projects/`](../projects/README.md).

## Booking requests

Frontend client: `services/booking-api.ts` (`bookingApi`). Swagger tags **Booking Requests** and **Vendor Booking Requests**. `plannerId` / `vendorId` always come from the JWT / vendor storefront.

Status transitions: DRAFT→PENDING, PENDING→ACCEPTED\|REJECTED\|CANCELLED, ACCEPTED→CONFIRMED, CONFIRMED→CANCELLED\|COMPLETED. Accept performs PENDING→ACCEPTED→CONFIRMED and creates the Order in one transaction.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/booking-requests` | PLANNER | Create PENDING. Availability check only — no reservation. 409 insufficient stock or duplicate PENDING. |
| GET | `/booking-requests` | PLANNER, ADMIN | Own requests (admin: all, optional `plannerId`/`vendorId`). Filter `status`, `projectId`. |
| GET | `/booking-requests/{id}` | PLANNER, ADMIN | Owner or admin. **404** if missing or not owned. Includes `orderId` when an order exists. |
| POST | `/booking-requests/{id}/cancel` | PLANNER | PENDING or CONFIRMED. CONFIRMED releases inventory unless the linked order is already out for delivery (409). |
| GET | `/vendor/booking-requests` | VENDOR | This vendor's inbox. |
| GET | `/vendor/booking-requests/stats` | VENDOR | Counts by status. |
| GET | `/vendor/booking-requests/{id}` | VENDOR | Records `VENDOR_VIEWED` once. **404** for other vendors. |
| POST | `/vendor/booking-requests/{id}/accept` | VENDOR | Re-check + CONFIRMED reservations + create Order. 409 if stock gone; request stays PENDING. |
| POST | `/vendor/booking-requests/{id}/reject` | VENDOR | `{ reason, note? }`. |

Error envelope is the same `{ status, error, message, fieldErrors }`. Examples: `"Only 70 units are currently available for the selected dates."`, `"Inventory is no longer available for the requested dates."`, `"A pending booking request already exists for this vendor, products, and dates."`

See [`docs/booking/`](../booking/README.md).

## Orders and delivery

Frontend client: `services/order-api.ts` (`orderApi`). Swagger tags **Orders**, **Vendor Orders**, **Admin Orders**, **Order Delivery**. `plannerId` / `vendorId` always come from the JWT / vendor storefront. Amounts are rental-value snapshots; payment is not collected. `totalAmount` = subtotal + optional delivery fee; `securityDeposit` is informational.

Status: PENDING→CONFIRMED→DELIVERY_SCHEDULED→OUT_FOR_DELIVERY→DELIVERED→EVENT_COMPLETED→PICKUP_SCHEDULED→PICKED_UP→COMPLETED. Cancel only from PENDING, CONFIRMED, DELIVERY_SCHEDULED.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/orders` | PLANNER, ADMIN | Own orders (admin: all). Filter `status`, `projectId`. |
| GET | `/orders/{id}` | PLANNER, ADMIN | Owner or admin. **404** if missing or not owned. |
| POST | `/orders/{id}/cancel` | PLANNER | Releases inventory. 409 if in transit or completed. |
| POST | `/orders/{id}/notes` | PLANNER, VENDOR, ADMIN | Owner-scoped notes. |
| GET | `/vendor/orders` | VENDOR | This vendor's orders. |
| GET | `/vendor/orders/calendar` | VENDOR | Today / upcoming / pickups / completed. Filter `date`, `status`, `city`. |
| GET | `/vendor/orders/{id}` | VENDOR | **404** for other vendors. |
| POST | `/vendor/orders/{id}/schedule-delivery` | VENDOR | CONFIRMED→DELIVERY_SCHEDULED. Venue + contact required. |
| POST | `/vendor/orders/{id}/out-for-delivery` | VENDOR | |
| POST | `/vendor/orders/{id}/mark-delivered` | VENDOR | |
| POST | `/vendor/orders/{id}/mark-event-completed` | VENDOR | |
| POST | `/vendor/orders/{id}/schedule-pickup` | VENDOR | Pickup cannot be before delivery. |
| POST | `/vendor/orders/{id}/mark-picked-up` | VENDOR | PICKED_UP→COMPLETED + inventory release. |
| GET | `/admin/orders` | ADMIN | All orders. |
| GET | `/admin/orders/{id}` | ADMIN | |
| GET | `/orders/{orderId}/delivery` | PLANNER, VENDOR, ADMIN | Planner view-only. |
| PUT | `/orders/{orderId}/delivery` | VENDOR, ADMIN | Upsert details; does not advance order status. |
| GET | `/orders/{orderId}/delivery/history` | PLANNER, VENDOR, ADMIN | |
| POST | `/orders/{orderId}/delivery/proof` | VENDOR, ADMIN | multipart `file`. Images + PDF, max 10 MB. |

See [`docs/orders/`](../orders/README.md) and [`docs/delivery/delivery-workflow.md`](../delivery/delivery-workflow.md).

## Notifications

Frontend client: `services/notification-api.ts` (`notificationApi`). Swagger tags **Notifications** and **Notification Preferences**. Users only see their own rows; other users' ids return **404**.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/notifications` | Authenticated | Filter `ALL` / `UNREAD` / `BOOKINGS` / `ORDERS` / `DELIVERY` / `INVENTORY` / `SYSTEM`. Paginated, newest first. |
| GET | `/notifications/unread-count` | Authenticated | `{ unreadCount }` |
| PATCH | `/notifications/{id}/read` | Authenticated | **404** if missing or not owned. |
| PATCH | `/notifications/read-all` | Authenticated | Marks the caller's unread rows. |
| DELETE | `/notifications/{id}` | Authenticated | **404** if missing or not owned. |
| GET | `/notification-preferences` | Authenticated | Defaults all enabled. |
| PUT | `/notification-preferences` | Authenticated | Replace booking/order/delivery/inventory/system flags. |

Domain events (`com.flowset.events`) create rows after the business transaction commits. See [`docs/notifications/`](../notifications/notification-architecture.md).

## Reviews

Frontend client: `services/review-api.ts` (`reviewApi`). Swagger tags **Reviews**, **Vendor Reviews**, **Admin Reviews**. `plannerId` / `vendorId` always come from the JWT and the order — never from the body for ownership. Stars are integers 1–5. See [`docs/reviews/`](../reviews/review-workflow.md).

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/vendors/{vendorId}/reviews` | Public | Published only. `rating`, `verified`, `sort` (`NEWEST`/`HIGHEST`/`LOWEST`), page, size. Includes summary + distribution. |
| GET | `/reviews/eligible` | PLANNER | Completed orders still in the create window with no review. |
| GET | `/reviews/eligible/{orderId}` | PLANNER | Includes `reviewPeriodExpired`. **404** if not owned. |
| GET | `/reviews` | PLANNER | Own reviews. Filter `status`, `orderId`. |
| GET | `/reviews/{id}` | Authenticated | Planner: own. Vendor: published about self. Admin: any. **404** otherwise. |
| POST | `/reviews` | PLANNER | Body: `orderId` + ratings + optional text. Creates `PUBLISHED` verified review. 409 duplicate / not completed / expired. 400 rating out of range. |
| PATCH | `/reviews/{id}` | PLANNER | Edit within 30 days (`flowset.reviews.edit-window-days`). 409 after window. |
| DELETE | `/reviews/{id}` | PLANNER | Soft delete. JSON `{ "reason" }`. Unique order_id still blocks a second review. |
| POST | `/reviews/{id}/report` | Authenticated | Not the author. Reasons: SPAM, ABUSIVE, FAKE, INAPPROPRIATE, PERSONAL_INFORMATION, OTHER. |
| GET | `/admin/reviews` | ADMIN | Filter `status`, `vendorId`, `plannerId`. |
| GET | `/admin/reviews/reported` | ADMIN | Open reports. |
| PATCH | `/admin/reviews/{id}/moderate` | ADMIN | `HIDE` / `RESTORE` / `REMOVE` / `NOTE`. Recalculates vendor rating except NOTE. |
| GET | `/admin/reviews/{id}/history` | ADMIN | Moderation trail. |

## Admin platform

Swagger tags are prefixed **Admin**. Every endpoint requires `bearerAuth` and `ADMIN`. See [`docs/admin/`](../admin/README.md). Financial report fields are **estimated rental value**, never collected revenue.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/admin/dashboard` | KPIs + trends. `range=TODAY\|DAYS_7\|DAYS_30\|DAYS_90\|CUSTOM` |
| GET | `/admin/users` | Search/filter. POST `/{id}/suspend`, `/reactivate`, `/reset-password` |
| GET | `/admin/vendors` | Verification queue. POST start-review, verify, reject, suspend, reactivate, feature, document review |
| GET | `/admin/products` | Moderation. POST approve, reject, suspend, restore, feature, unfeature |
| GET | `/admin/categories` | Full tree including archived. POST/PUT create/update, archive, reorder, feature, image |
| GET | `/admin/reports` | Vendor/product performance, bookings, estimated rental value, reviews, user growth |
| GET | `/admin/audit-logs` | Immutable. No DELETE. |
| GET/PUT | `/admin/settings` | Typed singleton |
| GET | `/admin/orders` | Support view |
| GET/PATCH | `/admin/support/tickets` | Assign, priority, reply |
| POST/GET | `/support/tickets` | Planner/vendor own tickets |

## Validation
