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

## Local JWT validation

The Run Service validates RS256 access tokens using the configured Identity Service public key. It does not use the private signing key.

The local profile supports:

| Environment variable | Default |
|---|---|
| `JWT_ISSUER` | `https://identity.pacesonline.local` |
| `JWT_PUBLIC_KEY_LOCATION` | `file:./config/keys/access-token-public.pem` |

The public-key location must point to the key corresponding to the Identity Service signing key. Shared local key provisioning is deferred to the local-integration work.

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
