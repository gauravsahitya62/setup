# Environment variables

Never commit real `.env` files. Copy `.env.example` for local work. Production and staging values live in **GitHub Actions Secrets** and **AWS Secrets Manager** (or SSM Parameter Store). The application reads standard environment variables; AWS injects them at task start.

## Secret storage

| Environment | Where values live |
| --- | --- |
| development | `.env.local` (gitignored) and `docker-compose*.yml` defaults |
| staging / production | AWS Secrets Manager → ECS task `secrets`, plus GitHub Environment secrets for deploy |

CI/CD must not use AWS root keys. Use a deploy IAM role with least privilege (ECR push, ECS update, optionally S3 for frontend if not on Vercel).

## Frontend (Vercel / Next.js)

| Variable | Required | Public | Notes |
| --- | --- | --- | --- |
| `NEXT_PUBLIC_APP_NAME` | yes | yes | Display name |
| `NEXT_PUBLIC_APP_URL` | yes | yes | Canonical site URL, no trailing slash |
| `NEXT_PUBLIC_APP_ENV` | yes | yes | `development` \| `staging` \| `production` |
| `NEXT_PUBLIC_API_BASE_URL` | yes | yes | Browser API, includes `/api/v1` |
| `API_INTERNAL_BASE_URL` | no | **no** | SSR inside Docker, e.g. `http://api:8080/api/v1` |
| `NEXT_PUBLIC_ENABLE_ANALYTICS` | no | yes | `true`/`false` |
| `NEXT_PUBLIC_ENABLE_DARK_MODE` | no | yes | `true`/`false` |
| `NEXT_PUBLIC_SENTRY_DSN` | no | yes | Empty until a real Sentry project exists |

Do not put JWT secrets, database passwords, or AWS keys in `NEXT_PUBLIC_*`.

## Backend (ECS / local)

| Variable | Required in prod | Notes |
| --- | --- | --- |
| `APPLICATION_ENV` | yes | `development`, `staging`, or `production` |
| `SPRING_PROFILES_ACTIVE` | yes | `dev`, `staging`, or `prod` |
| `DATABASE_URL` | yes | JDBC URL to RDS/local Postgres |
| `DATABASE_USERNAME` | yes | |
| `DATABASE_PASSWORD` | yes | |
| `JWT_ACCESS_SECRET` | yes | ≥32 chars, unique, not a documented example |
| `JWT_REFRESH_SECRET` | yes | Different from access secret |
| `JWT_ACCESS_EXPIRATION_MS` | no | Default 900000 (15m) |
| `JWT_REFRESH_EXPIRATION_MS` | no | Default 604800000 (7d) |
| `CORS_ALLOWED_ORIGINS` | yes | Comma-separated. Never `*` in staging/production |
| `COOKIE_SECURE` | yes | `true` in production |
| `COOKIE_SAME_SITE` | no | `None` when frontend and API are on different sites over HTTPS |
| `FRONTEND_URL` | no | Used for links in emails later |
| `STORAGE_PROVIDER` | yes | `s3` in production |
| `AWS_REGION` | yes if S3 | |
| `AWS_S3_BUCKET` | yes if S3 | |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | no | Prefer ECS task role |
| `HIKARI_MAX_POOL_SIZE` | no | Prod default 20 |
| `LOG_LEVEL` | no | `INFO` recommended |
| `RATE_LIMIT_ENABLED` | no | Default true except tests |
| `DEMO_SEED_ENABLED` | **never in production** | Seeds 5 planners / 10 vendors / 50 products. Staging or local only |
| `DEMO_SEED_PASSWORD` | if seed enabled | Shared demo password. Do not use for real users |

`REDIS_URL` is documented for future use. The MVP does not read it.

## Example origins

Development: `http://localhost:3000`  
Staging placeholder: `https://staging.flowset.in`  
Production placeholder: `https://app.flowset.in`

## Local Docker

`docker compose` sets development defaults. Override secrets via a gitignored `.env` next to the compose file (Compose auto-loads `.env`, not `.env.example`).
