# FlowSet

B2B marketplace and operating system for the event industry.

Event planners discover, compare, and request bookings from verified rental vendors.

## Stack

- **Frontend:** Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS, shadcn/ui
- **State / data:** Zustand, TanStack Query, React Hook Form, Zod, Axios
- **Backend:** Java 21, Spring Boot 3.5, Spring Security, JPA, Flyway, PostgreSQL, JWT
- **Docs:** `/docs`

## Getting started

### 1. Environment

```bash
cp .env.example .env.local
```

### 2. Database + API (Docker)

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

Or run the full stack:

```bash
docker compose up --build
```

### 3. Frontend

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

Auth pages: `/sign-in`, `/sign-up`, `/forgot-password`, `/reset-password`.

## Scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Start Turbopack dev server |
| `npm run build` | Production build |
| `npm run lint` | ESLint |
| `npm run typecheck` | TypeScript check |
| `cd backend && mvn test` | Backend unit + integration tests |

## Structure

```
app/                 # App Router
backend/             # Spring Boot modular monolith
components/
docs/                # Product + architecture docs
providers/           # Theme, Query, Auth
services/            # Axios API client + auth API
```

## Auth

See [docs/06-authentication.md](docs/06-authentication.md).
