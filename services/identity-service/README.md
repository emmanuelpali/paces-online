# Identity Service

The Identity Service is the authentication and user-account service for PacesOnline.

At this stage, the service only provides the initial Spring Boot application foundation. User registration, authentication, tokens, database integration, and other identity features will be added in later issues.

## Current scope

Issue #1 includes:

- A standalone Spring Boot application
- Spring Boot Actuator
- An HTTP health endpoint
- An application-context startup test
- Local build and run instructions

The following are intentionally not included yet:

- User entities
- Registration and login
- Spring Security configuration
- JWT access tokens
- Refresh tokens
- PostgreSQL
- Flyway migrations
- RabbitMQ
- Business logic

## Prerequisites

- Java 25
- Git

The project includes the Maven Wrapper, so a separate Maven installation is not required.

Verify your Java version:

```powershell
java --version
```

## Project location

Run all commands in this document from:

```text
paces-online/services/identity-service
```

From the repository root:

### Windows PowerShell

```powershell
cd services/identity-service
```

### macOS or Linux

```bash
cd services/identity-service
```

## Build and test

The following command compiles the application and runs the automated tests.

### Windows PowerShell

```powershell
.\mvnw.cmd clean verify
```

### macOS or Linux

```bash
./mvnw clean verify
```

A successful build ends with:

```text
BUILD SUCCESS
```

## Run locally

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS or Linux

```bash
./mvnw spring-boot:run
```

By default, the service starts on:

```text
http://localhost:8080
```

Keep this terminal running while testing the service.

## Verify service health

Spring Boot Actuator exposes the health endpoint at:

```text
GET /actuator/health
```

### Windows PowerShell

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

You can also inspect the full HTTP response:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

### macOS or Linux

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

A `200 OK` response with a status of `UP` confirms that the application started and the Actuator health endpoint is available.

## Run the tests only

### Windows PowerShell

```powershell
.\mvnw.cmd test
```

### macOS or Linux

```bash
./mvnw test
```

The current test verifies that the Spring application context starts successfully.

## Stop the service

In the terminal running the application, press:

```text
Ctrl+C
```

## Troubleshooting

### Port 8080 is already in use

Stop the application currently using port `8080`, or temporarily start the service on another port.

#### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Then check:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

### Incorrect Java version

Confirm that Java 25 is installed and active:

```powershell
java --version
```

Also confirm the Java compiler version:

```powershell
javac --version
```

### Clean a previous build

#### Windows PowerShell

```powershell
.\mvnw.cmd clean
```

#### macOS or Linux

```bash
./mvnw clean
```
## Configuration

The Identity Service uses Spring Boot profiles and externalized configuration.

### Profiles

The service currently supports:

- `local` — local development with safe developer defaults
- `test` — automated tests with deterministic test values
- `prod` — production/runtime configuration supplied externally

No profile is activated by default.

### Local Development

Activate the local profile before starting the service.

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
remove test profile Remove-Item Env:SPRING_PROFILES_ACTIVE

### Test Profile

Automated Spring Boot tests use the test profile.

Test-specific configuration is stored in:

src/test/resources/application-test.yml

Tests activate the profile using:

@ActiveProfiles("test")