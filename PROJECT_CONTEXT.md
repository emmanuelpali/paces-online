# PacesOnline Project Context

## Purpose

PacesOnline is a running-journal application and portfolio project.

Its primary goals are:

1. Develop practical intermediate Spring Boot skills.
2. Build practical Kubernetes experience supporting CKAD preparation.
3. Deliver a complete, credible full-stack portfolio application.

The project must prioritize learning and completion over architectural complexity.

Portfolio-ready Version 1 target:

**September 30, 2026**

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
5. Old handoffs and chat history are advisory only.

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

---

### Minimal Spring Boot BFF

The BFF is responsible for:

- Providing one API boundary for React
- Calling Identity Service and Run Service
- Using OpenAPI-generated Java clients for backend-service calls
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

BFF controllers remain handwritten.

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

## Spring Boot Learning Goals

Version 1 should provide practical experience with:

- Spring Boot application configuration
- Profiles and environment variables
- Type-safe application properties
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
- OpenAPI contracts and generated clients
- Actuator health endpoints
- Unit, MVC and integration testing
- Docker containerization
- Kubernetes deployment

Advanced patterns must not be added merely to demonstrate familiarity with them.

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

BFF controllers also remain handwritten.

Version 1 will not generate BFF server/controller interfaces.

Generated Java clients must not be edited manually.

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

## Testing and Learning Strategy

Testing is a primary learning goal, not a final cleanup activity.

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

### Testing Learning Process

For each significant feature:

1. Define a small behavior matrix together.
2. Select the correct test level for each behavior.
3. Write the first test with detailed guidance.
4. Have the user write subsequent tests with less scaffolding.
5. Review test names, setup, assertions and maintainability.
6. Explain why each test provides useful confidence.
7. Remove tests that merely repeat framework guarantees.

The goal is for the user to become comfortable deciding:

- What should be tested?
- At which level?
- What should be mocked?
- When is a real database necessary?
- What does the test actually prove?

---

## Knowledge Checks

Every major issue or milestone ends with a short knowledge check.

A knowledge check may include:

- Explaining an implemented concept in the user’s own words
- Reading a short code sample
- Predicting behavior
- Diagnosing a failure
- Explaining a tradeoff
- Writing or correcting a focused test
- Connecting the work to a Kubernetes concept

Knowledge checks should usually contain five to eight focused questions.

They test understanding, not memorization or obscure trivia.

If a knowledge gap appears:

1. Explain the concept.
2. Use a small targeted exercise.
3. Retest the concept.
4. Continue the project without adding unrelated scope.

Testing questions must be included regularly because automated testing is an explicit learning priority.

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

Docker Compose supports local execution of the complete system.

Only required infrastructure should be included.

Do not add containers for deferred technologies.

---

## Kubernetes and CKAD Strategy

Kubernetes is a core Version 1 goal, not a final optional addition.

The project should provide experience with:

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

CKAD exercises should also be practised independently rather than forcing every exam topic into the application.

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

### 6. Kubernetes and CKAD

- Deployments and Services
- ConfigMaps and Secrets
- Probes
- Resources
- Ingress
- Rollout practice
- Troubleshooting practice
- Optional NetworkPolicy
- Deployment documentation

### 7. Portfolio Release

- CI pipeline
- Architecture summary
- Screenshots or demonstration
- Final root README
- Final knowledge review

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
3. Does it teach an important intermediate Spring Boot or CKAD skill?
4. Is the learning value worth the delivery cost?
5. Can the same lesson be learned with a smaller implementation?

If the answers do not justify the complexity, do not add it.

Intermediate engineering is demonstrated through correct decisions and completed behavior—not maximum architecture complexity.
