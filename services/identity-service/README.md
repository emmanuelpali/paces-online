# PacesOnline Identity Service

The Identity Service owns user identity and authentication for PacesOnline.

Current capabilities:

* User registration
* Password hashing and verification
* PostgreSQL persistence
* Flyway database migrations
* User login
* RSA-signed JWT access-token issuance
* OpenAPI contract for Identity APIs

Refresh tokens, authenticated profile access, logout, and authorization are implemented in later issues.

## Requirements

* Java 25
* Docker
* Maven Wrapper included in the project

## Build and Test

From `services/identity-service`:

### Windows

```powershell
.\mvnw.cmd clean verify
```

### macOS / Linux

```bash
./mvnw clean verify
```

Database integration tests use Testcontainers and require Docker to be running.

## Configuration

Configuration is split by Spring profile:

```text
src/main/resources/
├── application.yml
├── application-local.yml
└── application-prod.yml

src/test/resources/
└── application-test.yml
```

Profiles are activated externally rather than being hardcoded in application configuration.

For local development:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
```

## PostgreSQL

The Identity Service uses PostgreSQL for persistence and Flyway for schema migrations.

Local defaults:

```text
Database: pacesonline_identity
Host: localhost
Port: 5432
Username: pacesonline
Password: pacesonline
```

The datasource can be overridden with:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

For example, if PostgreSQL is exposed on host port `5433`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"
```

Production datasource configuration has no fallback credentials and must be supplied externally.

## Flyway

Database migrations are stored under:

```text
src/main/resources/db/migration
```

Flyway applies pending migrations when the application starts against a configured datasource.

Hibernate validates JPA mappings against the Flyway-managed schema and does not create or update the schema.

## Running Locally

Start PostgreSQL, activate the local profile, and run:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

If PostgreSQL is running on a non-default host port:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"
.\mvnw.cmd spring-boot:run
```

The service runs on port `8080` by default.

The port can be overridden with:

```text
SERVER_PORT
```

Health is available at:

```text
GET /actuator/health
```

## User Registration

```text
POST /api/v1/auth/register
```

Example request:

```json
{
  "email": "runner@example.com",
  "password": "strong-password"
}
```

A successful registration returns `201 Created` with the user's ID, normalized email address, and creation timestamp.

Passwords are encoded using Spring Security's configured `PasswordEncoder`. Raw passwords and password hashes are never returned by the API.

Invalid requests return `400 Bad Request`.

Attempting to register an existing email returns `409 Conflict`.

## Login

```text
POST /api/v1/auth/login
```

Example request:

```json
{
  "email": "runner@example.com",
  "password": "strong-password"
}
```

Successful authentication returns `200 OK`:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Unknown users and incorrect passwords both return `401 Unauthorized` without revealing which credential was incorrect.

## JWT Access Tokens

Access tokens are signed using RSA with `RS256`.

Tokens include the standard claims:

```text
iss   issuer
sub   user UUID
iat   issued-at timestamp
exp   expiration timestamp
jti   unique token identifier
```

Issuer and expiration are configured through:

```text
paces-online.security.token.*
```

Local and test profiles generate temporary RSA key pairs when the application starts.

Production signing keys are supplied externally through:

```text
JWT_PRIVATE_KEY_LOCATION
JWT_PUBLIC_KEY_LOCATION
```

Production private keys must not be committed to the repository.

## OpenAPI

The Identity Service contract is maintained at:

```text
contracts/identity-api/openapi.yml
```

It currently documents:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Identity Service controllers are implemented by hand.

The contract will be consumed later by the BFF to generate its Identity Service Java client.

## Testing

The service uses several test levels:

```text
Unit tests
→ business behavior with isolated collaborators

MVC tests
→ HTTP requests, validation, response bodies, and status codes

Integration tests
→ Spring Boot + JPA + Flyway + PostgreSQL through Testcontainers
```

Run the complete suite with:

```powershell
.\mvnw.cmd clean verify
```

## Secrets

Do not commit production:

* database credentials
* JWT private keys
* other deployment secrets

Production values are supplied through the runtime environment and, later, Kubernetes configuration.
