# FlowSet

B2B marketplace and operating system for the event industry.

Event planners discover, compare, and request bookings from verified rental vendors: Project → Products → Booking Request → Vendor accept → Inventory reservation → Order → Delivery → Pickup → Review.

This repository is an MVP being prepared for a **controlled production pilot**. It is not a claim that AWS, DNS, or monitoring are already live.

## Architecture

```
Next.js (Vercel)  →  Spring Boot (AWS)  →  PostgreSQL (RDS)
                         ↓
                        S3
```

Modular monolith. Details: [`docs/02-architecture.md`](docs/02-architecture.md), [`docs/deployment/production-architecture.md`](docs/deployment/production-architecture.md).

| Layer | Stack |
| --- | --- |
| Frontend | Next.js 15 App Router, React 19, TypeScript, Tailwind |
| Backend | Java 21, Spring Boot 3.5, Spring Security, JPA, Flyway |
| Database | PostgreSQL 16 |
| Auth | BCrypt, JWT access (memory), HttpOnly refresh cookie |

## Prerequisites

- Node.js 22 and npm
- JDK 21 and Maven 3.9+
- Docker Desktop (PostgreSQL for local API)

## Environment setup

```bash
cp .env.example .env.local
```

Never commit `.env` or `.env.local`. Documented variables: [`docs/deployment/environment-variables.md`](docs/deployment/environment-variables.md).

Local JWT/database values in `.env.example` are **development placeholders**, not production secrets.

## Local development

### Database + API

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health/liveness
- Swagger (dev only): http://localhost:8080/swagger-ui.html

### Frontend

```bash
npm install
npm run dev
```

Open http://localhost:3000. Auth: `/sign-in`, `/sign-up`.

### Full Docker stack (API + Postgres + frontend image)

```bash
docker compose -f docker-compose.dev.yml up --build
```

The browser still calls `http://localhost:8080/api/v1`. Next.js SSR inside Docker uses `API_INTERNAL_BASE_URL=http://api:8080/api/v1`.

## Database

Flyway is the only schema source: `backend/src/main/resources/db/migration`. Do not edit production schema by hand. Recovery: [`docs/operations/database-recovery.md`](docs/operations/database-recovery.md).

## Tests

```bash
npm run lint
npm run typecheck
npm test
npm run build

cd backend
mvn test
python ../scripts/validate-flyway.py
```

## Demo seed (development / staging only)

Never enable in production. `ProductionSafetyStartup` refuses `DEMO_SEED_ENABLED=true` when `APPLICATION_ENV=production`.

```bash
# With the API already running against a disposable database:
DEMO_SEED_ENABLED=true DEMO_SEED_PASSWORD=DemoPassw0rd! mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Creates 5 planners (`demo.planner.1-5@flowset.local`), 10 verified vendors (`demo.vendor.1-10@flowset.local`), 50 products, projects, and one accepted sample booking. Idempotent if `demo.planner.1@flowset.local` exists.

## Staging smoke

```bash
SMOKE_FRONTEND_URL=http://localhost:3000 SMOKE_API_URL=http://localhost:8080 npm run smoke
```

## Building

```bash
# Frontend production build
npm run build

# Backend jar
cd backend && mvn -DskipTests package

# Images
docker build -t flowset-web .
docker build -t flowset-api ./backend
```

## Deployment overview

| Environment | How |
| --- | --- |
| PR | GitHub Actions CI (must pass) |
| Staging | Push to `develop` or `staging` after CI |
| Production | Manual `workflow_dispatch` + GitHub Environment approval |

Guide: [`docs/deployment/deployment-guide.md`](docs/deployment/deployment-guide.md). Rollback: [`docs/deployment/rollback.md`](docs/deployment/rollback.md).

## Troubleshooting

| Problem | Check |
| --- | --- |
| API will not start in prod/staging | JWT secrets ≥32 chars, CORS not `*`, `COOKIE_SECURE=true` in production |
| Login cookie missing on HTTPS split domains | `COOKIE_SAME_SITE=None` and `COOKIE_SECURE=true` |
| CORS errors | `CORS_ALLOWED_ORIGINS` must match the exact frontend origin |
| File upload rejected | Magic bytes must match MIME; no SVG/HTML/JS/exe; max 10MB |
| 429 Too Many Requests | Auth endpoints are rate limited (~20/min/IP/instance) |
| Marketplace empty | Vendor published + product published; admin verification may apply |
| `next build` on Windows after standalone | Delete `.next` if Next.js files in `node_modules` look corrupted, then rebuild |
| Docker commands fail | Start Docker Desktop; the daemon was not running in some local environments |

## Documentation

- [`docs/security/access-control.md`](docs/security/access-control.md)
- [`docs/security/security-checklist.md`](docs/security/security-checklist.md)
- [`docs/operations/runbook.md`](docs/operations/runbook.md)
- [`docs/operations/monitoring.md`](docs/operations/monitoring.md)
- [`docs/operations/smoke-test.md`](docs/operations/smoke-test.md)
- [`docs/06-authentication.md`](docs/06-authentication.md)
