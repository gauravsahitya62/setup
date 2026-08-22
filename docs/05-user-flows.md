# FlowSet User Flows — Authentication

## Register planner

1. User opens `/sign-up?role=planner` (default planner).
2. Submits first name, last name, email, phone, password, company name.
3. Backend creates `users` + `planner_profiles`, issues email verification token (logged, not emailed in v1).
4. Returns tokens; frontend stores access token, cookie holds refresh token.
5. Redirect to `/planner`.

## Register vendor

1. User opens `/sign-up?role=vendor`.
2. Submits account fields + business name.
3. Backend creates `users` + `vendor_profiles` (verification pending).
4. Redirect to `/vendor`.

## Login

1. User opens `/sign-in`.
2. Submits email + password.
3. On success, role-based redirect: planner → `/planner`, vendor → `/vendor`, admin → `/admin`.
4. On failure, show generic invalid credentials (no user enumeration beyond validation messages where appropriate).

## Session restore

1. App loads; AuthProvider calls refresh (cookie) then `/auth/me` if access token missing.
2. Authenticated users hitting `/sign-in` or `/sign-up` redirect to their dashboard.

## Logout

1. Client calls `/auth/logout` (revokes refresh token).
2. Clears memory session and cookie.
3. Redirect to `/sign-in`.

## Forgot / reset password

1. `/forgot-password` submits email → always returns success message; token logged server-side.
2. `/reset-password` with token + new password updates hash and revokes refresh tokens.

## Protected routes

Middleware / client guards protect `/planner/*`, `/vendor/*`, `/admin/*` by required role.

## Planner projects

1. Planner opens `/planner/projects/new` (or Quick add on the dashboard).
2. Enters name, event type, optional date/guests/venue/cover. Save as draft or create (`PLANNING`).
3. Workspace at `/planner/projects/{id}`: overview, products, vendors, mood board, notes, files, activity, booking requests, orders.
4. Marketplace **Add to Project** picks an existing project or prompts to create the first one. From `/planner/projects/{id}/marketplace`, adds go to that project.
5. Adding a product or saving a vendor does **not** send a booking request or reserve inventory.
6. **Request booking** (`/planner/projects/{id}/booking`) sends a PENDING request to one vendor. Repeat for other vendors. Vendors accept/reject at `/vendor/requests`. Accept reserves inventory and creates an Order.
7. Planner tracks fulfillment at `/planner/orders`. Vendor fulfills at `/vendor/bookings` (schedule delivery → delivered → pickup → completed).
8. After an order is **COMPLETED**, the planner can leave one verified review (`/planner/reviews` or the order page) for 30 days. The public vendor profile and marketplace cards show the live aggregate (never `0.0` when there are no reviews). Admins moderate at `/admin/reviews`.
9. The bell in planner/vendor/admin shells shows unread in-app notifications. Inbox: `/planner/notifications`, `/vendor/notifications`, `/admin/notifications` (or `/notifications` which redirects by role). Preferences live on each role's Settings page.

Details: [`docs/projects/project-workflow.md`](./projects/project-workflow.md), [`docs/booking/booking-workflow.md`](./booking/booking-workflow.md), [`docs/orders/order-workflow.md`](./orders/order-workflow.md), [`docs/reviews/review-workflow.md`](./reviews/review-workflow.md), and [`docs/notifications/notification-architecture.md`](./notifications/notification-architecture.md).
