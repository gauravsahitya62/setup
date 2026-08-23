# Production architecture (pilot)

FlowSet remains a **modular monolith**. This document describes the intended first-pilot topology. Domain names below are placeholders until DNS is purchased and verified.

```
Browser
   │
   ▼
Cloudflare (DNS, TLS, CDN for public assets)
   │
   ├─ app.flowset.in  →  Vercel (Next.js)
   └─ api.flowset.in  →  AWS (Spring Boot)
                           │
                           ├─ Amazon RDS PostgreSQL (private)
                           ├─ Amazon S3 (uploads)
                           └─ CloudWatch (logs/metrics)
```

## Chosen compute

**Amazon ECS on Fargate, one service, one task definition** is the simplest AWS runtime that still supports:

- image deploys
- health checks
- graceful shutdown
- IAM task roles
- no Kubernetes

An EC2 VM with Docker Compose is an acceptable fallback if Fargate is not yet available. Do not introduce EKS.

## Components

| Layer | Pilot choice | Notes |
| --- | --- | --- |
| Frontend | Next.js on Vercel | `NEXT_PUBLIC_*` only. No backend secrets. |
| Backend | Spring Boot 3.5 / Java 21 | Single JAR, `SPRING_PROFILES_ACTIVE=prod` |
| Database | RDS PostgreSQL 16 | Private subnet, encryption at rest, automated backups |
| Files | S3 | Bucket private. Public catalog images served through `/api/v1/files` after path classification |
| Cache | None | Redis is **not** in the current application. Do not add it only for rate limiting. |
| Edge | Cloudflare | DNS + HTTPS. Do not cache authenticated API responses. |
| Secrets | GitHub Actions secrets + AWS Secrets Manager | Never commit production `.env` files |
| CI/CD | GitHub Actions | PR CI; staging after `develop`/`staging`; production is manual |

## Environments

| Name | Frontend (placeholder) | API (placeholder) | Data |
| --- | --- | --- | --- |
| development | http://localhost:3000 | http://localhost:8080 | Local Docker PostgreSQL |
| staging | https://staging.flowset.in | https://api.staging.flowset.in | Separate RDS + S3 |
| production | https://app.flowset.in | https://api.flowset.in | Production RDS + S3 |

`flowset.in` is a placeholder. Do not assume the domain is registered or that DNS/TLS already exist.

## Why not more infrastructure

Kafka, Elasticsearch, Kubernetes, and a second Redis cluster would add operational cost without changing the current booking workflow. Add them only when a measured bottleneck requires it.

## Connection pooling

Production HikariCP defaults (`application-prod.yml`):

- `maximum-pool-size=20`
- `minimum-idle=5`
- `max-lifetime` slightly under typical RDS idle timeouts

Reasoning: one Fargate task plus headroom for a rolling second task and Flyway. `db.t3.micro`/`small` `max_connections` is far higher than 20. Raise the pool only after measuring RDS `DatabaseConnections`.

## Storage model

`StorageService` is the only upload path.

- `STORAGE_PROVIDER=local` for development
- `STORAGE_PROVIDER=s3` for staging/production
- Objects are stored with SSE-S3 and are not public-read
- Private prefixes (`vendors/*/documents`, `projects/*`, `orders/*`) require authentication and ownership checks in `StorageAccessService`

## Rate limiting

In-memory per instance (`RateLimitFilter`). Limits are not shared across replicas. For a single-task pilot this is acceptable. Documented limitation if replica count grows.

## Caching

Public marketplace/vendor SSR uses `fetch(..., { next: { revalidate: 30 } })` (30-second ISR). That cache is **public catalog data only**, not user-specific. Invalidation is time-based; a newly published product can take up to 30 seconds to appear on SSR pages. Client-side TanStack Query remains the interactive source of truth after login.

Do not cache authenticated API responses at Cloudflare.

- AWS account, VPC, RDS, S3, ECS, IAM, Secrets Manager
- Cloudflare zone and certificates
- Vercel project
- GitHub Environment protection rules
- Real monitoring dashboards
