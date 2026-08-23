# Rollback

MVP does **not** require perfect zero downtime. Prefer a short maintenance window over unsafe automatic database reverse-migrations.

## Frontend (Vercel)

1. In the Vercel dashboard, promote the previous successful deployment.
2. Confirm `NEXT_PUBLIC_API_BASE_URL` still points at the intended API.
3. Hard-refresh a logged-out marketing page and a signed-in planner page.

## Backend (ECS / image)

1. Redeploy the previous image digest (ECR tag such as `git-<sha>`).
2. Wait for the target group to mark the replacement healthy (`/actuator/health/readiness`).
3. Watch 5xx and login failures for 10 minutes.

Do not `latest`-only tag in production. Tag every image with the git SHA.

## Database

**Do not automatically roll back Flyway migrations.**

- Forward-only migrations are the default (`Add → migrate app → remove later`).
- If a release includes a breaking schema change, it must be split: additive migration in release N, code switch in N or N+1, drop in a later release.
- Restore from RDS snapshot / PITR only for data corruption or catastrophic failed migration. See `docs/operations/database-recovery.md`.

## Feature flags / config

If the incident is configuration (CORS, cookie flags, S3 bucket), revert the ECS task environment and force a new deployment. That is faster than a code rollback.

## Decision guide

| Symptom | First action |
| --- | --- |
| Frontend 5xx / blank page | Rollback Vercel deployment |
| API 5xx after image roll | Rollback ECS image |
| Auth cookie failures | Check `COOKIE_SECURE`, `COOKIE_SAME_SITE`, CORS origins |
| Migration failed at boot | Keep old image running; do not start the new task; repair with a follow-up additive migration |
| Data loss / corruption | PITR / snapshot restore (maintenance window) |
