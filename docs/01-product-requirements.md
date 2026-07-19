# FlowSet Product Requirements

## Vision

FlowSet is a B2B SaaS marketplace and operating system for the event industry. Event planners discover, compare, and request bookings from verified rental vendors.

## Personas

| Persona | Goal |
| --- | --- |
| Planner | Source inventory, manage projects, request bookings |
| Vendor | Manage catalog, respond to requests, fulfill bookings |
| Admin | Verify vendors, moderate products, operate the platform |

## Core modules (product surface)

1. Marketing / landing
2. Planner dashboard & project workspace
3. Marketplace discovery
4. Public vendor profiles
5. Vendor operations dashboard
6. Admin panel
7. Authentication & authorization (this milestone)

## Auth requirements

- Register as Planner or Vendor
- Login / logout with JWT access + refresh tokens
- Forgot / reset password
- Email verification (placeholder delivery)
- Role-based routing to planner, vendor, or admin dashboards
- Soft-delete users; never hard-delete account rows in v1

## Non-goals (v1 auth)

- Social OAuth
- Multi-factor authentication
- Organization / team invites (beyond single-user registration)

## Success criteria

- Production-grade Spring Security configuration
- Flyway-managed PostgreSQL schema
- Documented OpenAPI
- Frontend wired to real APIs (no mock auth)
- Runnable via Docker Compose when the daemon is available
