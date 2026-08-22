# Review workflow

```
Order COMPLETED
  → REVIEW_ELIGIBLE notification (planner)
  → Planner eligible if: owner, COMPLETED, no review, within create window
  → POST /api/v1/reviews  (orderId + ratings; identity from JWT)
  → Review PUBLISHED, is_verified = true
  → VendorRatingService recalculates vendors.rating / review_count
  → REVIEW_RECEIVED (planner: published confirmation; vendor: “new verified review” — no review body)
  → Public GET /api/v1/vendors/{vendorId}/reviews
```

## Planner

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/reviews/eligible` | Completed orders still reviewable |
| GET | `/reviews/eligible/{orderId}` | Includes expired window flag |
| GET | `/reviews` | Own reviews (`status`, `orderId`, page) |
| GET | `/reviews/{id}` | Own review; others 404 |
| POST | `/reviews` | Create. 409 duplicate / not completed / expired |
| PATCH | `/reviews/{id}` | Edit inside window |
| DELETE | `/reviews/{id}` | Soft delete + reason body |
| POST | `/reviews/{id}/report` | Not the author |

Frontend: `/planner/reviews` (to complete / published / history), order details `OrderReviewPrompt`, `/planner/orders` “Leave a Review” only while eligible.

## Public

`GET /api/v1/vendors/{vendorId}/reviews?rating&verified&sort&page&size`

Sort: `NEWEST` (default), `HIGHEST`, `LOWEST`. Only `PUBLISHED`. Paginated. Response includes `summary` (average, count, verified count, 1–5 distribution). Planner display name is first name only plus initials.

## Vendor

Vendors read published reviews via the public list (dashboard uses `/vendors/me` then `/vendors/{id}/reviews`). They cannot PATCH/DELETE. Hidden reviews 404 for vendors.

## Notifications

| Type | When |
| --- | --- |
| `REVIEW_ELIGIBLE` | Order completed |
| `REVIEW_RECEIVED` | Review published (planner + vendor; vendor copy does not include review text) |
| `REVIEW_REPORTED` | Admins |
| `REVIEW_MODERATED` | Planner and vendor owner after hide/restore/remove/note |

`REVIEW_REMINDER` remains a template-only type (no scheduler).
