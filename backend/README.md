# FlowSet Backend

Spring Boot 3.5 modular monolith for FlowSet.

## Run locally

1. Start Postgres (Docker Compose from repo root):

```bash
docker compose up -d postgres
```

2. Export secrets (see root `.env.example`) and run:

```bash
cd backend
mvn spring-boot:run
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

## Tests

```bash
mvn test
```

Tests use an in-memory H2 database (`application-test.yml`).
