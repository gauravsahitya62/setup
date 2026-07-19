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
