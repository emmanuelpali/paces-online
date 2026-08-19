# PacesOnline Identity Service

The Identity Service owns user identity and authentication for PacesOnline.

Current capabilities:

- User registration
- Password hashing and verification
- PostgreSQL persistence with Flyway migrations
- User login
- RSA-signed JWT access-token issuance
- JWT access-token validation
- Authenticated user profile access
- OpenAPI contract for Identity APIs

Refresh tokens, logout, and role-based authorization are planned for later issues.

## Requirements

- Java 25
- Docker
- Maven Wrapper included in the project

## Build and Test

From `services/identity-service`:

```powershell
.\mvnw.cmd clean verify
```

On macOS/Linux:

```bash
./mvnw clean verify
```

Integration tests use Testcontainers, so Docker must be running.

## Configuration

Spring profiles:

```text
src/main/resources/
├── application.yml
├── application-local.yml
└── application-prod.yml

src/test/resources/
└── application-test.yml
```

Profiles are activated externally.

For local development:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
```

## PostgreSQL

Local defaults:

```text
Database: pacesonline_identity
Host: localhost
Port: 5432
Username: pacesonline
Password: pacesonline
```

Datasource overrides:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Example for PostgreSQL exposed on host port `5433`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"
```

Production datasource values must be supplied externally.

## Database Migrations

Flyway migrations are stored under:

```text
src/main/resources/db/migration
```

Flyway owns schema evolution. Hibernate validates JPA mappings against the migrated schema rather than creating or updating it.

## Running Locally

Start PostgreSQL, activate the local profile, and run:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

The service runs on port `8080` by default. Override it with:

```text
SERVER_PORT
```

Health endpoint:

```text
GET /actuator/health
```

## API

### Register User

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

A successful registration returns `201 Created`.

Invalid requests return `400 Bad Request`. Registering an existing email returns `409 Conflict`.

Passwords are encoded with Spring Security's configured `PasswordEncoder`. Raw passwords and password hashes are never returned by the API.

### Login

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

Unknown users and incorrect passwords both return `401 Unauthorized`.

### Authenticated User Profile

```text
GET /api/v1/users/user
```

Send the access token as:

```text
Authorization: Bearer <access-token>
```

The endpoint returns the profile belonging to the authenticated user.

The user's UUID comes from the validated JWT `sub` claim and is used to load the current profile from PostgreSQL.

Example response:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "runner@example.com",
  "createdAt": "2026-08-18T15:00:00Z"
}
```

Missing or invalid access tokens return `401 Unauthorized`. Password information is never included in the response.

## JWT

Access tokens are signed with RSA using `RS256`.

Tokens contain:

```text
iss   issuer
sub   user UUID
iat   issued-at timestamp
exp   expiration timestamp
jti   unique token identifier
```

Issuer and token lifetimes are configured through:

```text
paces-online.security.token.*
```

Local and test profiles generate temporary RSA key pairs at application startup.

Production signing keys are supplied externally through:

```text
JWT_PRIVATE_KEY_LOCATION
JWT_PUBLIC_KEY_LOCATION
```

Production private keys must not be committed to the repository.

Protected endpoints use Spring Security OAuth2 Resource Server support. Incoming Bearer tokens are validated for:

- RSA signature using the trusted public key
- expiration
- issuer

Authentication is stateless; clients send the Bearer token with each protected request.

## OpenAPI

The Identity Service contract is maintained at:

```text
contracts/identity-api/openapi.yml
```

Current operations:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/users/user
```

The authenticated profile operation uses the `bearerAuth` JWT security scheme.

Identity Service controllers are handwritten. The contract will later be consumed by the BFF to generate its Identity Service Java client.

## Testing

The service uses:

- unit tests for isolated business behavior
- MVC tests for HTTP validation, security behavior, responses, and status codes
- integration tests with Spring Boot, Spring Security, JPA, Flyway, PostgreSQL, and Testcontainers

The integration suite covers the authenticated profile flow with a real signed JWT and rejection of missing, malformed, expired, wrong-issuer, and untrusted-key tokens.

Run the full suite with:

```powershell
.\mvnw.cmd clean verify
```

## Secrets

Do not commit production:

- database credentials
- JWT private keys
- other deployment secrets

Production values are supplied through the runtime environment and, later, Kubernetes configuration.
