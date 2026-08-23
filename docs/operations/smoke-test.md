# Production smoke test

Run against **staging first**, then production after a deploy. Do **not** seed production with development fixtures.

Automated coverage of much of this path already exists in backend `*IntegrationTest` classes. This checklist is for a live environment.

## 0. Health

```bash
bash scripts/healthcheck.sh https://api.example.com
```

## Core workflow

1. Application loads (marketing homepage, no console errors that block render).
2. Planner registration.
3. Planner login (access token in memory, refresh cookie set).
4. Vendor registration.
5. Vendor login.
6. Vendor profile creation (publish only if the environment allows public visibility).
7. Product creation with at least one variant and inventory quantity.
8. Product marketplace visibility (`GET /search` or marketplace UI) — requires published vendor + published product.
9. Planner creates a Project.
10. Planner adds the Product to the Project.
11. Planner sends a Booking Request.
12. Vendor accepts.
13. Inventory reservation exists for the booking window.
14. Order is created from the confirmed booking.
15. Vendor schedules delivery.
16. Delivery marked delivered (with proof upload).
17. Pickup completed / order completion path.
18. Planner submits a review.
19. Vendor rating updates on the public vendor profile.
20. Notification appears for the planner and/or vendor.

## AuthZ probes (quick)

- Planner A cannot `GET` planner B’s project (404/403).
- Vendor A cannot read vendor B’s documents.
- Planner cannot `PATCH` inventory.
- Non-admin `GET /api/v1/admin/dashboard` → 403.

## Files

- Product image URL loads without auth.
- Vendor GST/PAN URL without auth → 401.
- Owner or admin can open the private document.

## Stop conditions

If steps 12–14 fail, treat the environment as **not ready for planners**. Frontend-only issues can be rolled back independently (`docs/deployment/rollback.md`).
