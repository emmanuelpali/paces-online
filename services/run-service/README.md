# Run Service

The Run Service owns run persistence for PacesOnline.

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