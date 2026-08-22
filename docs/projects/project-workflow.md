# Project workflow

## Planner path (this module)

```
Create Project
  → Enter event details (name, type, date, guests, venue)
  → Browse marketplace (optionally /planner/projects/{id}/marketplace)
  → Add product variants to the project
  → Save vendors
  → Build mood board
  → Add notes and files
  → Review requirements and planning progress
  → Send booking requests          ← Prompt 13
  → Track booking status           ← Prompt 13
  → Confirmed orders / delivery    ← later
```

## Create

1. Planner authenticates.
2. `POST /api/v1/projects` with name + event type (draft allowed). Cover upload is `POST /projects/{id}/cover`.
3. Backend stamps `planner_id` from the security context, generates a unique slug per planner, creates an empty mood board, and writes `PROJECT_CREATED` activity.
4. Default status is `DRAFT`. The wizard's "Create project" sets `PLANNING`; "Save as draft" keeps `DRAFT`.

## Marketplace integration

- Product detail and marketplace cards expose **Add to Project**.
- If the planner has no projects, the dialog prompts **Create your first project**.
- If they have projects, they pick one (or create another).
- From `/planner/projects/{id}/marketplace`, the current project is implied.
- Success adds a `project_products` row. **No booking request is created.**

## Workspace tabs

Overview · Products · Vendors · Mood Board · Notes · Files · Activity · Booking Requests (`/planner/projects/{id}/booking` to send a request).

## Booking requests

Implemented in `com.flowset.planner.booking`. Group project products by vendor, `POST /booking-requests`, then `InventoryAvailabilityService.reserveInventory` only when the vendor accepts. See [`docs/booking/booking-workflow.md`](../booking/booking-workflow.md).
