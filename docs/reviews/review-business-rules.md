# Review business rules

Reviews are vendor-level trust signals. Product ratings are not a separate system in this MVP; the completed `orders` row retains product association for later analytics.

## Who can review

1. Only the planner on a **COMPLETED** order can create a review for that vendor.
2. A review cannot be created before the order is completed.
3. A planner cannot review a vendor they have never booked (ownership is the order’s `planner_id`, taken from the JWT).
4. One review per order. `UNIQUE (order_id)` remains after soft delete so a second review cannot replace a removed one.

`planner_id`, `vendor_id`, and order ownership are never trusted from the client. They are copied from the authenticated user and the order row.

## Ratings

- Overall rating is required: integer **1–5** (`SMALLINT`).
- Communication, product quality, delivery, and value for money are optional integers 1–5.
- Review text is optional, max `flowset.reviews.max-text-length` (default 2000).
- Frontend-supplied averages are ignored. Persistence and aggregates are validated on the server.

See [ADR 0004](../architecture/decisions/0004-review-rating-storage.md) for types.

## Status

| Status | Meaning |
| --- | --- |
| `PENDING` | Reserved for a future pre-publish queue. MVP create skips this. |
| `PUBLISHED` | Visible on the public profile; included in vendor aggregates. |
| `HIDDEN` | Admin hide. Not public, not in aggregates. |
| `REMOVED` | Soft-deleted (planner request or admin remove). |
| `REPORTED` | Available for admin holding; reporting itself does **not** change visibility. |

Reporting writes `review_reports` and leaves the review `PUBLISHED` until an admin hides or removes it. That prevents a single false report from dropping a vendor’s rating.

## Verified

Every review created from a completed order has `is_verified = true`. Public UI shows **Verified Booking**.

## Edit window

Planners may edit their own review for `flowset.reviews.edit-window-days` (default **30**) after `created_at`. After that the review is read-only.

The same window from `orders.completed_at` is the create window. After it expires the planner sees **Review period expired** and cannot submit.

## Delete

Planner delete is a **soft** delete: `deleted_at`, `deleted_by`, `delete_reason`, status `REMOVED`. No hard delete. Unique `order_id` still blocks a replacement.

Vendors cannot edit or delete reviews about themselves.

## Vendor replies

Not implemented. Nullable `vendor_response_text` / `vendor_response_at` exist for a later reply feature.

## Aggregates

`VendorRatingService` recomputes `vendors.rating` (`NUMERIC(3,2)`) and `vendors.review_count` from **PUBLISHED** and non-deleted reviews after publish, edit, hide, restore, or remove. Marketplace cards read those cached columns. Vendors with zero reviews must not display `0.0` stars — UI shows **No reviews yet**.
