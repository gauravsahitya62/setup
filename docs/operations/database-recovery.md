# Database recovery

Production PostgreSQL is intended to run on **Amazon RDS**. This procedure is written for RDS. Local Docker volumes are not a backup strategy.

## What AWS should have enabled

Configure (and verify in the AWS console — not assumed here):

- Automated backups, retention **at least 7 days** for the pilot
- Backup window during low traffic
- Encryption at rest
- Point-in-time recovery (PITR) if the instance class/engine supports it
- Deletion protection on production

## Verify backups exist

1. RDS → instance → Maintenance & backups.
2. Confirm latest automated snapshot age is within the retention window.
3. Optionally create a manual snapshot named `flowset-pre-change-YYYYMMDD` before risky operations.

This check is **external**. It cannot be proven from this repository.

## Restore to a new instance (preferred)

Restoring in place is more dangerous. Restore to a **new** RDS instance, then re-point the application.

1. Choose snapshot or PITR timestamp.
2. Restore to `flowset-restore-YYYYMMDD`.
3. Same VPC/subnets; new security group copy if needed.
4. Confirm `SELECT flyway_schema_history.version FROM flyway_schema_history ORDER BY installed_rank;` matches the expected release.
5. Update Secrets Manager `DATABASE_URL` (and user/password if rotated).
6. Force a new ECS deployment so connections refresh.
7. Run `docs/operations/smoke-test.md` against staging first if the restore is a drill.

## What happens during rollback

- In-flight bookings can fail while the API cannot reach the database (readiness fails; ECS stops sending traffic if the load balancer is configured correctly).
- Restoring to a timestamp **before** a booking was accepted can resurrect inventory that was later reserved. After restore, run a consistency check:
  - `inventory_reservations` vs `booking_requests.status`
  - `orders.booking_request_id` uniqueness
- Do not run a Flyway “undo”. If schema is ahead of code, deploy the matching application version instead.

## Application-level notes

- Flyway is the only schema source (`backend/src/main/resources/db/migration`).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate will not migrate.
- Connection pool: see `docs/deployment/production-architecture.md`. After restore, connections must be recycled (task restart).

## Local/dev

`docker compose down -v` destroys the local volume. That is not production recovery.
