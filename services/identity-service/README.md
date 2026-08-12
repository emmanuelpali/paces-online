# PacesOnline Identity Service

The Identity Service is the authentication and identity boundary for PacesOnline.

At the current stage of the project, the service provides the Spring Boot foundation and configuration strategy that later authentication and persistence work will build on.

## Current Capabilities

- Standalone Spring Boot application
- Spring Boot Actuator
- Health, liveness, and readiness endpoints
- Profile-based configuration
- Type-safe token configuration with `@ConfigurationProperties`
- Configuration validation and fail-fast startup
- Automated configuration tests

Authentication, JWT generation, PostgreSQL persistence, and user management are not implemented yet.

## Requirements

- Java 25
- Maven Wrapper included in the repository

## Build and Test

From the `services/identity-service` directory:

### Windows PowerShell

```powershell
.\mvnw.cmd clean verify
```

### macOS or Linux

```bash
./mvnw clean verify
```

A successful build should end with:

```text
BUILD SUCCESS
```

## Run Locally

The Identity Service requires token configuration. For local development, use the `local` profile.

### Windows PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

### macOS or Linux

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

By default, the service starts at:

```text
http://localhost:8080
```

When finished in PowerShell, remove the profile environment variable:

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

## Health Endpoints

With the service running, the main health endpoint is:

```text
GET /actuator/health
```

Example:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

The service also exposes Spring Boot liveness and readiness health groups for future container and Kubernetes probes.

## Configuration

The service uses Spring Boot externalized configuration.

Configuration is split across:

```text
src/main/resources/
├── application.yml
├── application-local.yml
└── application-prod.yml

src/test/resources/
└── application-test.yml
```

### Base Configuration

`application.yml` contains configuration shared across environments, including:

- Application name
- Server port
- Actuator endpoint exposure

No Spring profile is hardcoded as active.

### Local Profile

`application-local.yml` contains safe developer defaults.

The local token configuration currently includes:

```yaml
paces-online:
  security:
    token:
      issuer: paces-online-local
      access-token-expiration: 15m
      refresh-token-expiration: 7d
```

These values are intended only for local development and must never contain real production secrets.

### Test Profile

Automated Spring Boot tests use the `test` profile.

Test configuration is stored in:

```text
src/test/resources/application-test.yml
```

The application-context test activates it using:

```java
@ActiveProfiles("test")
```

The test profile contains deterministic values so tests do not depend on a developer's local environment.

### Production Profile

Activate the production profile externally:

```text
SPRING_PROFILES_ACTIVE=prod
```

The production profile requires token configuration to be supplied through environment variables:

```text
JWT_ISSUER
JWT_ACCESS_TOKEN_EXPIRATION
JWT_REFRESH_TOKEN_EXPIRATION
```

Example non-secret values:

```text
JWT_ISSUER=paces-online
JWT_ACCESS_TOKEN_EXPIRATION=15m
JWT_REFRESH_TOKEN_EXPIRATION=7d
```

The production profile intentionally provides no fallback values for required token settings.

If required configuration is missing or invalid, the application fails during startup.

In a future Kubernetes or OpenShift deployment, runtime values will be supplied externally through mechanisms such as ConfigMaps and Secrets.

## Token Configuration

Application-specific token settings are bound to `TokenProperties` using:

```java
@ConfigurationProperties(prefix = "paces-online.security.token")
```

The properties are represented using appropriate Java types:

- `issuer` → `String`
- `accessTokenExpiration` → `Duration`
- `refreshTokenExpiration` → `Duration`

Configuration validation ensures that:

- The issuer is not blank
- Access-token expiration is present and greater than zero
- Refresh-token expiration is present and greater than zero
- Refresh-token expiration is longer than access-token expiration

Invalid configuration prevents the application from starting.

This issue defines token configuration policy only. JWT generation, signing, verification, and signing-key configuration are implemented in later work.

## Server Port

The HTTP port can be overridden with:

```text
SERVER_PORT
```

Example in PowerShell:

```powershell
$env:SERVER_PORT="9090"
```

If `SERVER_PORT` is not provided, the service uses port `8080`.

## Secrets

Real credentials and secrets must never be committed to Git.

This includes:

- Passwords
- JWT signing keys
- Private keys
- Access tokens
- Refresh tokens
- Database credentials

Production secrets must be supplied by the deployment environment.

## Project Context

PacesOnline is intentionally kept focused on two goals:

1. Build a strong intermediate-level full-stack portfolio project.
2. Reinforce practical Kubernetes skills for CKAD preparation.

The project prefers built-in Spring Boot and Kubernetes capabilities over unnecessary custom abstractions.

### Local PostgreSQL

The Identity Service expects PostgreSQL on port `5432` by default.

If port `5432` is already in use, expose the PostgreSQL container on another
host port and override `DB_URL`.

Example:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/pacesonline_identity"