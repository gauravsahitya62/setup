# Deployment guide

This is the runbook for a **controlled pilot**. It does not claim that AWS, Vercel, or Cloudflare are already provisioned.

## Prerequisites

- GitHub repository with Actions enabled
- Protected `master`/`main` (no force-push, required CI)
- GitHub Environments `staging` and `production` (production: required reviewers)
- AWS account with permission to create VPC, RDS, S3, ECS, IAM, Secrets Manager, CloudWatch
- Vercel project for the Next.js app
- Cloudflare zone **after** the domain is purchased

Placeholder hostnames: `app.flowset.in`, `api.flowset.in`, `staging.flowset.in`.

## 1. Database (RDS)

1. Create PostgreSQL 16 in a private subnet.
2. Enable encryption at rest, automated backups (7+ days), and PITR.
3. Security group: allow 5432 only from the ECS task security group.
4. Create database `flowset` and a least-privilege user.
5. Store URL/username/password in Secrets Manager.

Flyway runs on application startup. Do not change production schema in a GUI.

## 2. Object storage (S3)

1. Create a private bucket (block public access on).
2. Enable default encryption.
3. IAM task role: `s3:PutObject`, `GetObject`, `DeleteObject` on `bucket/prefix*` only.
4. Set `STORAGE_PROVIDER=s3`, `AWS_S3_BUCKET`, `AWS_REGION`.

## 3. Backend (ECS Fargate)

1. Build and push the image from `backend/Dockerfile` to ECR.
2. Task CPU/memory: start `512/1024`.
3. Inject secrets from Secrets Manager.
4. `SPRING_PROFILES_ACTIVE=prod`, `APPLICATION_ENV=production`, `COOKIE_SECURE=true`.
5. Health check: `/actuator/health/liveness` and `/actuator/health/readiness`.
6. Target group: HTTPS listener. Do not expose actuator beyond health.
7. Desired count: 1 for the first pilot. Rate limits are in-memory.

## 4. Frontend (Vercel)

1. Set `NEXT_PUBLIC_APP_ENV`, `NEXT_PUBLIC_APP_URL`, `NEXT_PUBLIC_API_BASE_URL`.
2. Production domain: `app.flowset.in` (placeholder).
3. Preview/staging: separate Vercel environment.

## 5. Cloudflare

1. Proxied A/CNAME for app and API.
2. Full (strict) TLS once origin certificates exist.
3. Cache: bypass for `/api/*` and any `Set-Cookie` / `Authorization` responses.
4. CDN cache only public marketing/static assets.

## 6. CI/CD

- Pull requests run `.github/workflows/ci.yml` (lint, typecheck, tests, build, Flyway version check, Maven verify).
- Merge to `develop` or `staging` runs CI then the staging workflow placeholder.
- Production: GitHub Action `Deploy production` with `workflow_dispatch` confirmation `deploy-production` **and** Environment approval.

Wire the placeholder deploy jobs to `aws ecs update-service` and `vercel deploy --prod` only after credentials exist.

## 7. First deploy order

1. RDS + secrets
2. S3 + IAM
3. Backend image + ECS (Flyway migrates)
4. Confirm `/actuator/health/readiness`
5. Frontend
6. Cloudflare DNS
7. Manual smoke test (`docs/operations/smoke-test.md`)

## 8. IAM (least privilege)

- App task role: S3 object actions on one bucket; CloudWatch logs; Secrets Manager `GetSecretValue` for named secrets only.
- Deploy role: ECR, ECS update, PassRole to the task role.
- Never put AWS root credentials in GitHub or `application.yml`.
