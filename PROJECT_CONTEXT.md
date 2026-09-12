# PacesOnline Project Context

## Purpose

PacesOnline is a production-minded running-journal application and full-stack reference implementation.

Its primary goals are:

1. Deliver a complete and secure user workflow for recording and managing runs.
2. Maintain clear service boundaries and contract-first integrations.
3. Provide repeatable builds, automated verification and containerized deployment.
4. Deploy and operate the system on a local Kubernetes cluster.

The project prioritizes correctness, maintainability and completion over architectural complexity.

September 30, 2026 is an initial planning target, not a hard deadline.

Correctness, review quality and a complete core workflow take priority over meeting that date. The Version 1 scope remains fixed rather than expanding when the date moves.

A smaller application that is secure, tested, documented and deployable is more valuable than a larger unfinished system.

---

## Source of Truth

This file records stable project decisions.

Use:

```text
PROJECT_CONTEXT.md
        ↓
GitHub issue
        ↓
implementation and tests
        ↓
commit and pull request
```

Information belongs in:

| Information | Location |
|---|---|
| Goals, architecture and stable decisions | `PROJECT_CONTEXT.md` |
| Active implementation scope | GitHub issue |
| Requests, responses and status codes | OpenAPI |
| Build, configuration and operation | README |
| Expected behavior and edge cases | Automated tests |
| Implementation history | Commits and pull requests |

Do not duplicate the same detailed information across these sources.

When sources disagree:

1. A newly approved decision supersedes an older decision.
2. This file defines stable project direction.
3. The active GitHub issue defines current scope.
4. The current branch defines actual implementation state.
5. Superseded documents and implementation notes are advisory only.

---

## Core Workflow

Version 1 must support:

```text
Register
    ↓
Log in
    ↓
Create a run
    ↓
View run history
    ↓
View, update or delete a run
    ↓
Log out
```

A user must never be able to read or modify another user’s runs.

This workflow takes priority over optional features.

---

## Architecture

```text
React + TypeScript
        |
        v
Minimal Spring Boot BFF
        |
        +--------------------------+
        |                          |
        v                          v
Identity Service              Run Service
        |                          |
        v                          v
Identity data                 Run data
```

The repository is a monorepo.

Each Spring Boot application remains independently buildable, testable, containerized and deployable.

The BFF is retained as a small integration layer. It must not become another domain service.

Spring Boot applications use conventional layers when those layers have real responsibilities:

```text
Controller -> Service -> Repository -> PostgreSQL
```

Controllers own the HTTP boundary, services own business rules and transaction boundaries, and repositories own persistence queries. API DTOs remain separate from JPA entities. Empty layers and service interfaces without a concrete need must not be created merely to satisfy a template.

The BFF follows the same dependency discipline but has no repository because it owns no database.

---

## Application Responsibilities

### React Frontend

The frontend is responsible for:

- Registration and login forms
- Authenticated session handling
- Run creation
- Run editing
- Run history
- User feedback and validation
- Sending access tokens to the BFF

The frontend communicates only with the BFF.

React keeps the short-lived access token in memory. The refresh token must not be exposed to frontend JavaScript or stored in browser storage.

---

### Minimal Spring Boot BFF

The BFF is responsible for:

- Providing one API boundary for React
- Calling Identity Service and Run Service
- Using OpenAPI-generated Java clients for backend-service calls
- Implementing generated Spring API interfaces from the handwritten BFF OpenAPI contract
- Keeping controller implementations handwritten
- Managing the Identity Service refresh token through a configurable HttpOnly, SameSite cookie
- Returning the short-lived access token to React for in-memory use
- Rotating and clearing the refresh-token cookie during refresh and logout
- Forwarding access tokens to protected downstream endpoints
- Translating a small set of downstream failures
- Centralizing frontend-facing CORS configuration
- Reading service locations from external configuration
- Exposing an Actuator health endpoint

The BFF must not:

- Own a database
- Contain domain business logic
- Issue or validate passwords
- Issue access or refresh tokens
- Persist user sessions
- Query service databases
- Add caching or messaging
- Add retries or circuit breakers in Version 1
- Add distributed tracing infrastructure
- Aggregate responses unless a real frontend requirement appears
- Develop its own complex security model

BFF controller implementations remain handwritten and implement generated Spring API interfaces. Generated interfaces and DTOs must not contain business or integration logic.

---

### Identity Service

The Identity Service owns:

- User registration
- Password hashing
- Authentication
- JWT access-token issuance
- JWT validation for its protected endpoints
- Refresh-token issuance and rotation
- Token reuse detection and family revocation
- Authenticated profile information
- Minimal logout

The Identity Service owns its data.

Issue #7 already demonstrates substantial security, transaction and concurrency depth. Version 1 must not add more advanced identity functionality unless required by the core workflow.

---

### Run Service

The Run Service owns:

- Run creation
- Run retrieval
- Run updates
- Run deletion
- Run history
- Date filtering
- Run-type filtering
- Pace calculation
- Run ownership enforcement

The Run Service validates JWT access tokens using the configured public key.

It obtains the authenticated user ID from the validated JWT `sub` claim.

Every read or mutation must enforce ownership. For example:

```java
findByIdAndUserId(runId, authenticatedUserId)
```

The Run Service must never query Identity Service database tables.

It does not need to call Identity Service for every authenticated request.

---

## Version 1 Features

### Identity

Version 1 includes:

- Register with email and password
- Log in
- Receive an access token
- Receive a refresh token
- Refresh a session
- View the authenticated profile
- Log out by revoking the refresh-token family

Logout remains intentionally small:

1. Accept a refresh token.
2. Hash it.
3. Locate its family.
4. Revoke active tokens in that family.
5. Return `204 No Content`.

Version 1 does not include:

- Administrator workflows
- Role-management workflows
- Account disabling
- Logout from all devices
- Device tracking
- Session-management UI
- Refresh-token cleanup jobs
- Password-reset workflows
- Complex security auditing

---

### Run Management

A user can:

- Create a run
- View one of their runs
- View run history
- Update one of their runs
- Delete one of their runs
- Filter runs by date
- Filter runs by run type
- Add optional notes

Initial run fields:

```text
id
userId
startedAt
runType
distanceKilometres
durationSeconds
averagePace
perceivedEffort
notes
createdAt
updatedAt
```

Initial run types:

```text
EASY
RECOVERY
LONG
TEMPO
INTERVAL
RACE
```

Average pace is calculated by the backend from distance and duration.

The client must not provide the authoritative calculated pace.

---

## Engineering Capabilities

Version 1 demonstrates:

- Spring Boot application configuration
- Profiles and environment variables
- Type-safe application properties
- Conventional controller, service and repository layering
- REST controllers
- Request validation
- Consistent exception handling
- Service-layer business logic
- Spring Data JPA
- PostgreSQL
- Flyway migrations
- Transactions
- Spring Security
- JWT authentication
- Ownership-based authorization
- Downstream HTTP clients
- OpenAPI contracts, generated clients and generated server interfaces
- Actuator health endpoints
- Unit, MVC and integration testing
- Docker containerization
- Kubernetes deployment

Advanced patterns must not be added without a concrete product or operational requirement.

---

## OpenAPI Strategy

Backend services maintain handwritten OpenAPI contracts.

```text
Identity OpenAPI
        ↓
Generated Identity Java client
        ↓
BFF
```

```text
Run OpenAPI
        ↓
Generated Run Java client
        ↓
BFF
```

Backend service controllers remain handwritten.

The BFF maintains a small handwritten public contract for React.

That contract generates Spring server interfaces and API DTOs. Handwritten BFF controllers implement the generated interfaces.

The Identity and Run contracts generate the Java clients used by the BFF.

Generated sources are build output, are not committed, and must not be edited manually. Generator versions and important options are pinned so builds remain reproducible.

Generating a TypeScript client for React is optional and should be added only if it saves implementation effort.

Detailed API request and response documentation belongs in OpenAPI rather than service READMEs.

---

## Database Strategy

PostgreSQL is the source of truth.

Each backend service owns its logical data boundary.

Services must not directly query each other’s tables.

Flyway owns schema creation and evolution.

Hibernate validates mappings against the Flyway-managed schema. Automatic Hibernate schema updates are not the production migration strategy.

API DTOs remain separate from persistence entities.

Do not introduce repository abstractions beyond Spring Data unless a concrete requirement justifies them.

---

## Configuration Strategy

Use Spring Boot’s standard configuration model where possible:

```text
spring.datasource.*
server.*
management.*
logging.*
```

Use custom `@ConfigurationProperties` only for application-specific concepts.

Examples include:

```text
paces-online.security.token.*
paces-online.clients.identity.*
paces-online.clients.runs.*
```

Production configuration and secrets are supplied externally.

Kubernetes uses:

- ConfigMaps for non-sensitive configuration
- Secrets for credentials and signing-key locations

Do not wrap standard Spring Boot configuration in custom abstractions without a real requirement.

---

## Testing Strategy

Testing is part of feature delivery, not a final cleanup activity.

Before implementing a feature:

1. Identify the expected behaviors.
2. Identify important failure cases.
3. Decide which behaviors require unit, MVC or integration tests.
4. Implement tests alongside the production code.
5. Avoid testing trivial framework behavior.

### Unit Tests

Use unit tests for isolated business rules such as:

- Pace calculation
- Validation decisions not handled by annotations
- Ownership decisions
- Token-generation behavior
- Service behavior with mocked collaborators

### MVC Tests

Use MVC tests for:

- Request validation
- JSON mapping
- Status codes
- Controller-to-service interaction
- Authentication requirements
- Error-response behavior

### Integration Tests

Use PostgreSQL and Testcontainers when real framework or database behavior matters:

- Flyway migrations
- JPA mappings
- Repository queries
- Transactions
- Ownership enforcement
- Security-filter behavior
- Important end-to-end service flows

Do not use Testcontainers for behavior that a small unit test can prove adequately.

### BFF Tests

BFF tests should focus on:

- Correct downstream request construction
- Authorization-header propagation
- Response mapping
- Important downstream error translation

Do not duplicate all Identity and Run Service tests in the BFF.

### Test Design Process

For each significant feature:

1. Define the expected behavior and important failure cases.
2. Select the smallest appropriate test level for each behavior.
3. Implement tests alongside production code.
4. Review test names, setup, assertions, isolation and maintainability.
5. Run the relevant test suite before merge.
6. Remove tests that merely repeat framework guarantees.

Tests must make ownership, security, persistence and API behavior visible to reviewers.

---

## Documentation Strategy

Documentation must remain concise.

### Root README

The final root README should contain:

- Project purpose
- Architecture summary
- Technology stack
- Local startup instructions
- Test instructions
- Kubernetes deployment instructions
- Links to OpenAPI contracts
- Portfolio screenshots or demo material

### Service README

Each service README should normally contain only:

- Purpose
- Requirements
- Build and run commands
- Configuration variables
- Database migration information
- Endpoint list
- OpenAPI contract location
- Test command

Detailed API schemas belong in OpenAPI.

Detailed edge cases belong in tests.

Stable decisions belong in this file.

Do not create documentation solely because a template exists.

---

## Docker Strategy

Each deployable application receives one Docker image:

- Identity Service
- Run Service
- BFF
- React frontend

All application images use consistent container standards: reproducible builds, minimal runtime images, non-root execution and externally supplied configuration. Each Dockerfile remains tailored to its workload.

Docker Compose supports local execution of the complete system.

Only required infrastructure should be included.

Do not add containers for deferred technologies.

---

## Kubernetes Strategy

Kubernetes is a core Version 1 deployment target.

The Version 1 deployment target is a local Kubernetes cluster using images published to GitHub Container Registry. A public cloud deployment, DNS and production TLS are outside the Version 1 scope.

Kubernetes manifests include:

- Namespaces
- Deployments
- Services
- ConfigMaps
- Secrets
- Liveness probes
- Readiness probes
- Resource requests
- Resource limits
- Ingress
- Environment-based configuration
- Rolling updates
- Troubleshooting with logs and events
- NetworkPolicies if time permits
- Kustomize only when it simplifies environment overlays

Kubernetes manifests must remain small and understandable.

Do not introduce:

- Helm unless a real packaging requirement appears
- Service mesh
- Operators
- Complex deployment platforms
- Multiple competing deployment frameworks


---

## Observability

Version 1 observability is intentionally small:

- Spring Boot Actuator
- Health endpoint
- Liveness
- Readiness
- Useful application logs

Prometheus, Grafana and advanced observability platforms are not required.

---

## CI

Version 1 requires one understandable CI pipeline that:

- Builds each application
- Runs automated tests
- Fails on build or test errors
- Builds container images when appropriate
- Publishes releasable images to GitHub Container Registry with traceable tags

Do not introduce multiple CI/CD systems.

---

## Revised Delivery Sequence

### 1. Complete Identity Service

- Merge refresh-token work
- Implement minimal logout
- Add Identity Service Dockerfile
- Stop adding Identity features

### 2. Build Run Service

- Bootstrap and configuration
- PostgreSQL and Flyway
- JWT validation
- Run persistence
- Run CRUD
- Filtering
- Pace calculation
- Ownership enforcement
- Meaningful automated tests
- OpenAPI contract
- Dockerfile

### 3. Build Minimal BFF

- Maintain a small handwritten BFF OpenAPI contract
- Generate BFF Spring server interfaces and API DTOs
- Implement the generated interfaces with handwritten controllers
- Generate backend Java clients
- Configure backend service locations
- Implement thin authentication endpoints
- Implement thin run endpoints
- Propagate authorization headers
- Add focused integration tests
- Add Actuator and Dockerfile

### 4. Build React Frontend

- Registration
- Login
- Session handling
- Run creation
- Run history
- Run editing
- Run deletion
- Logout

### 5. Local Integration

- Docker Compose
- PostgreSQL
- Complete local startup
- End-to-end core workflow
- Concise operating documentation

### 6. Kubernetes Deployment

- Deployments and Services
- ConfigMaps and Secrets
- Probes
- Resources
- Ingress
- Rollout practice
- Troubleshooting practice
- Optional NetworkPolicy
- Deployment documentation

### 7. Release Readiness

- CI pipeline
- Architecture summary
- Screenshots or demonstration
- Final root README
- End-to-end acceptance verification

---

## Explicitly Out of Scope

The following are deferred beyond Version 1:

- Redis
- RabbitMQ
- Kafka
- Notification workers
- Email notifications
- MinIO
- S3 integration
- Run photos
- Administrator workflows
- Role-management workflows
- Account administration
- Complex security auditing
- Refresh-token cleanup jobs
- Device tracking
- Advanced analytics
- Prometheus and Grafana stack
- Elasticsearch
- Service mesh
- Event sourcing
- GPS tracking
- Route maps
- Wearable integrations
- Social feeds
- Followers
- Likes
- Comments
- Running clubs
- Challenges
- Training plans
- Payments
- Native mobile applications
- AI coaching

---

## Complexity Rule

Before adding a technology, abstraction, layer or framework, ask:

1. What concrete Version 1 problem does it solve?
2. Does Spring Boot, React, PostgreSQL, Docker or Kubernetes already solve it?
3. Does it improve correctness, security, maintainability or operability?
4. Is the engineering benefit worth the delivery and operational cost?
5. Can the requirement be met with a smaller implementation?

If the answers do not justify the complexity, do not add it.

Engineering quality is demonstrated through sound decisions and completed behavior, not maximum architectural complexity.
