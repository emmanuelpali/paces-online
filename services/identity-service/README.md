# PacesOnline Identity Service

The Identity Service owns user identity and authentication for PacesOnline.

## Capabilities

- User registration
- Password hashing and verification
- PostgreSQL persistence
- Flyway database migrations
- User authentication
- RSA-signed JWT access-token issuance
- JWT access-token validation
- Authenticated user-profile access
- Opaque refresh-token issuance
- Refresh-token rotation
- Refresh-token reuse detection and family revocation
- Handwritten OpenAPI contract

Logout will be implemented separately. Role-management and administrator workflows are outside the Version 1 scope.

## Requirements

- Java 25
- Docker
- Maven Wrapper included in the project

Docker must be running when executing integration tests because the test suite uses PostgreSQL through Testcontainers.

## Build and Test

Run commands from `services/identity-service`.

### Windows

```powershell
.\mvnw.cmd clean verify
```

### macOS and Linux

```bash
./mvnw clean verify
```

The verification build:

- Compiles the application
- Runs unit tests
- Runs MVC tests
- Starts PostgreSQL through Testcontainers
- Applies Flyway migrations
- Runs database integration tests
- Validates the Identity Service OpenAPI contract and its references

## Configuration

The service uses Spring profiles:

```text
src/main/resources/
├── application.yml
├── application-local.yml
└── application-prod.yml

src/test/resources/
└── application-test.yml
```

Profiles are activated externally rather than being hardcoded.

### Local profile

On Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
```

On macOS or Linux:

```bash
export SPRING_PROFILES_ACTIVE=local
```

Production configuration and secrets must be supplied through the runtime environment.

## PostgreSQL

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

Example for PostgreSQL exposed on port `5433`:

### Windows

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"
```

### macOS and Linux

```bash
export DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"
```

Production datasource configuration has no fallback credentials and must be supplied externally.

## Database Migrations

Flyway migrations are stored under:

```text
src/main/resources/db/migration
```

Current migrations create:

- The `users` table
- The `refresh_tokens` table
- Required constraints and indexes

Flyway owns schema creation and evolution.

Hibernate validates the JPA mappings against the migrated schema. It does not create or update the production database schema.

## Running Locally

Start PostgreSQL, activate the local profile, and run the service.

### Windows

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

### macOS and Linux

```bash
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

The service runs on port `8080` by default.

Override the port with:

```text
SERVER_PORT
```

The health endpoint is available at:

```text
GET /actuator/health
```

## API

The Identity Service currently provides:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /api/v1/users/user
```

Detailed request schemas, response schemas, validation requirements, and status codes are defined in the OpenAPI contract.

### Registration

```text
POST /api/v1/auth/register
```

Registers a user using an email address and password.

Passwords are encoded using Spring Security’s configured `PasswordEncoder`. Raw passwords and password hashes are never returned by the API.

Expected results:

```text
201 Created      registration succeeded
400 Bad Request  request validation failed
409 Conflict     email is already registered
```

### Login

```text
POST /api/v1/auth/login
```

Authenticates a registered user and returns:

- A short-lived JWT access token
- A long-lived opaque refresh token
- The token type
- Access-token expiration information
- Refresh-token expiration information

Unknown users and incorrect passwords both return `401 Unauthorized` without revealing which credential was incorrect.

An unsuccessful login does not create a refresh token.

### Refresh Session

```text
POST /api/v1/auth/refresh
```

Exchanges a valid refresh token for:

- A new JWT access token
- A rotated opaque refresh token
- Updated expiration information

Each refresh token can be used successfully only once. After a successful refresh, the client must discard the previous refresh token and store the newly returned token.

If a consumed refresh token is presented again, the service treats it as possible token theft or replay and revokes the active token family.

Unknown, expired, revoked, and previously consumed refresh tokens return the same generic `401 Unauthorized` response.

### Authenticated User Profile

```text
GET /api/v1/users/user
```

The request must include a valid access token:

```text
Authorization: Bearer <access-token>
```

The endpoint returns the profile belonging to the authenticated user.

The user UUID comes from the validated JWT `sub` claim and is used to load the profile from PostgreSQL.

Missing, malformed, expired, wrong-issuer, or incorrectly signed access tokens return `401 Unauthorized`.

Passwords and password hashes are never included in the profile response.

## Access Tokens

Access tokens are JWTs signed with RSA using `RS256`.

Tokens contain these standard claims:

```text
iss   token issuer
sub   user UUID
iat   issued-at timestamp
exp   expiration timestamp
jti   unique token identifier
```

Issuer and token lifetimes are configured through:

```text
paces-online.security.token.*
```

Local and test profiles generate temporary RSA key pairs when the application starts.

Production signing keys are supplied through:

```text
JWT_PRIVATE_KEY_LOCATION
JWT_PUBLIC_KEY_LOCATION
```

Production private keys must never be committed to the repository.

Protected endpoints use Spring Security OAuth2 Resource Server support. Incoming Bearer tokens are validated for:

- RSA signature
- Expiration
- Issuer

Authentication is stateless. Clients send the access token with each protected request.

## Refresh Tokens

Refresh tokens are cryptographically secure opaque values containing 256 bits of randomness.

They do not contain user information or authorization claims.

The service returns the raw refresh token only when the token is created. PostgreSQL stores only its SHA-256 digest.

Raw refresh tokens must never be:

- Persisted
- Written to logs
- Included in exceptions
- Included in error responses

Each successful login creates a new token family representing one authenticated session. A user can have separate token families for different sessions.

Rotating a token does not extend the family expiration time. When the family expires, the user must authenticate again.

Refresh-token rotation is transactional. PostgreSQL locking prevents two concurrent requests from successfully rotating the same token.

Consumed and revoked records remain available so token reuse can be detected.

## OpenAPI

The handwritten Identity Service contract is maintained at:

```text
contracts/identity-api/identity-api.yaml
```

The contract is divided into reusable path, schema, and security-scheme files under:

```text
contracts/identity-api/
├── identity-api.yaml
├── paths/
└── components/
```

The complete contract is validated during the Maven verification build. The validation test confirms that the root document is valid and external references resolve.

Identity Service controllers remain handwritten.

The Spring Boot BFF will later generate its Identity Service Java client from this contract. Generated client code must not be edited manually.

## Testing

The service uses several levels of automated testing.

### Unit tests

Unit tests cover isolated behavior such as:

- Registration logic
- Login logic
- Access-token generation
- Refresh-token generation
- SHA-256 token hashing
- Refresh-token rotation
- Expiration handling
- Revocation handling
- Reuse detection

### MVC tests

MVC tests cover:

- HTTP request validation
- Response bodies
- Status codes
- Public and protected endpoint behavior
- Generic unauthorized responses

### PostgreSQL integration tests

Integration tests use Spring Boot, Spring Security, JPA, Flyway, PostgreSQL, and Testcontainers.

They cover:

- Database migrations
- User persistence
- Password hashing
- Login and token issuance
- JWT validation
- Refresh-token digest persistence
- Refresh-token rotation
- Fixed family expiration
- Consumed-token reuse detection
- Family revocation
- Expired-token rejection
- Unsuccessful-login persistence behavior
- Concurrent refresh-token rotation

## Security Notes

The service follows these security rules:

- Raw passwords are never persisted.
- Password hashes are never returned.
- Raw refresh tokens are never persisted.
- Token digests are never exposed through the API.
- Authentication failures use generic responses.
- Production signing keys are supplied externally.
- Database credentials and private keys are not committed.
- Access tokens are short-lived.
- Refresh tokens are single-use and rotated.
- Replay detection revokes the affected token family.

## Secrets

Do not commit:

- Production database credentials
- JWT private keys
- Raw refresh tokens
- API secrets
- Deployment credentials

Production values will be supplied through the runtime environment and, later, Kubernetes Secrets.