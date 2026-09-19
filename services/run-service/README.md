# Run Service

The Run Service owns run persistence and ownership-aware run lookups for PacesOnline.

## Requirements

- Java 25
- Docker
- PostgreSQL 17

## Local database

Start PostgreSQL:

```powershell
docker run --name pacesonline-run-postgres `
  -e POSTGRES_DB=pacesonline_runs `
  -e POSTGRES_USER=pacesonline `
  -e POSTGRES_PASSWORD=pacesonline `
  -p 5432:5432 `
  -d postgres:17-alpine
```

The local configuration defaults can be overridden with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## Run locally

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The service starts on port `8081` by default. `SERVER_PORT` can override it.

Check application health:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

## Tests

Docker must be running because integration tests use PostgreSQL through Testcontainers.

```powershell
.\mvnw.cmd clean verify
```

## Database schema

Flyway owns schema creation and applies migrations from `src/main/resources/db/migration`.

Hibernate validates the JPA mappings against the migrated schema and does not create or update tables.
