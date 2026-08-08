# PacesOnline Project Context

## Purpose

This file is the durable source of truth for the PacesOnline project.

It records stable decisions about:

- Project goals
- Version 1 scope
- Architecture
- Engineering standards
- Delivery priorities

Detailed implementation work belongs in the active GitHub issue.

Git commits record implementation progress.

Handoff documents are optional and should only be created when they provide meaningful continuity between development sessions or conversations.

---

# Project Summary

PacesOnline is a running journal application.

A user can:

- Register
- Log in
- Record completed runs
- View and manage their own runs
- Filter their run history

The application is primarily a portfolio and learning project.

Its two highest priorities are:

1. Build a strong intermediate-level full-stack engineering portfolio project.
2. Provide practical Kubernetes experience that reinforces CKAD preparation.

The project must not become a collection of technologies added only for learning or résumé keywords.

---

# Target Release

Portfolio-ready Version 1 target:

**September 30, 2026**

Quality is more important than feature count.

Version 1 must demonstrate one complete workflow that is:

- Functional
- Secure
- Tested
- Documented
- Containerized
- Deployable to Kubernetes

A smaller finished application is more valuable than a larger unfinished architecture.

---

# Scope Rule

Before adding a technology, service, abstraction, library, or infrastructure component, ask:

> What concrete problem does this solve for the Version 1 workflow or CKAD goal?

Also ask:

> Does the framework or platform already solve this problem?

If an existing Spring Boot, React, PostgreSQL, Docker, or Kubernetes feature already solves the problem adequately, prefer that feature instead of creating a custom abstraction.

Do not add technology solely to demonstrate familiarity with it.

---

# Version 1 Core Workflow

```text
Register
    ↓
Log in
    ↓
Create a run
    ↓
View run history
    ↓
View / update / delete one of your runs
    ↓
Log out
```

A user must never be able to read or modify another user's private runs.

That complete workflow takes priority over optional features.

---

# Version 1 Features

## Identity and Access

A user can:

- Register using email and password
- Log in
- Receive an access token
- Receive a refresh token
- Refresh an authenticated session
- Log out
- View their own profile

The Identity Service is responsible for:

- User registration
- Password hashing
- Authentication
- Access-token generation
- Refresh-token handling
- Logout and token revocation
- Authenticated-user identity

Version 1 does not require:

- Administrator workflows
- Role-management screens
- Account-administration features
- A complex security-audit subsystem

Authorization must be sufficient to protect authenticated endpoints and prevent cross-user access.

---

## Run Management

A user can:

- Create a run
- View one of their runs
- Update one of their runs
- Delete one of their runs
- View run history
- Filter runs by date
- Filter runs by run type
- Add optional notes

Average pace is calculated by the backend from distance and duration.

The client must not be trusted to provide the final calculated pace.

### Run Fields

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

### Initial Run Types

```text
EASY
RECOVERY
LONG
TEMPO
INTERVAL
RACE
```

Statistics, photos, social functionality, and similar enhancements are not required for Version 1.

---

# Version 1 Architecture

```text
                     React + TypeScript
                            |
                            v
                     Spring Boot BFF
                            |
              -----------------------------
              |                           |
              v                           v
       Identity Service              Run Service
              |                           |
              v                           v
         PostgreSQL                  PostgreSQL
```

The repository is a monorepo.

Each application remains independently buildable and deployable.

---

# Application Responsibilities

## React Frontend

Responsible for:

- Registration
- Login
- Authenticated session handling
- Run creation
- Run editing
- Run history
- User feedback and validation

The frontend communicates with the BFF.

The frontend must not directly depend on internal backend-service locations.

---

## Spring Boot BFF

Responsible for:

- Providing a frontend-oriented API
- Calling Identity Service and Run Service
- Using OpenAPI-generated Java clients
- Hiding internal service locations
- Propagating authenticated context safely
- Translating backend failures into useful frontend responses
- Performing limited response aggregation when genuinely useful

The BFF must remain thin.

Domain business logic belongs in the service that owns that domain.

---

## Identity Service

Responsible for:

- Registration
- Password hashing
- Login
- Access tokens
- Refresh tokens
- Logout and token revocation
- Authenticated profile information

The Identity Service owns its own PostgreSQL data.

---

## Run Service

Responsible for:

- Run creation
- Run retrieval
- Run updates
- Run deletion
- Run history
- Filtering
- Pace calculation
- User ownership enforcement

The Run Service owns its own PostgreSQL data.

The Run Service must never query Identity Service database tables directly.

---

# Architecture Decisions

The following decisions are currently locked for Version 1:

- The project is named PacesOnline.
- The repository may remain named `aces-online` until intentionally renamed.
- The project uses a monorepo.
- React and TypeScript are used for the frontend.
- A Spring Boot BFF sits between the frontend and backend services.
- Identity Service and Run Service are separate Spring Boot applications.
- Each service is independently buildable and deployable.
- Each backend service owns its own data.
- Services do not directly query another service's database.
- PostgreSQL is the source of truth.
- Flyway manages database schema migrations.
- Spring Data/JPA may be used for application persistence.
- OpenAPI contracts define backend APIs.
- OpenAPI Generator generates Java clients used by the BFF.
- Business logic is written manually.
- Persistence entities are not generated.
- API DTOs remain separate from persistence entities.
- Kubernetes supplies runtime configuration externally.
- Spring Boot remains responsible for configuration binding.
- Kubernetes ConfigMaps are used for non-secret runtime configuration.
- Kubernetes Secrets are used for sensitive runtime configuration.
- One container image is built per deployable application.

Do not introduce additional architectural layers without a concrete problem requiring them.

---

# Spring Boot Configuration Decisions

Use Spring Boot's existing configuration model whenever possible.

Examples:

```text
spring.datasource.*
server.*
management.*
logging.*
```

Do not wrap standard Spring Boot configuration in custom classes without a real application-specific requirement.

For PostgreSQL, use Spring Boot datasource configuration rather than a custom `DatabaseProperties` abstraction.

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pacesonline
    username: pacesonline
    password: ...
```

Custom `@ConfigurationProperties` classes are appropriate for PacesOnline-specific concepts.

Example:

```text
TokenProperties
```

for:

```text
paces-online.security.token.*
```

Production configuration and secrets are supplied externally.

---

# Database Decisions

PostgreSQL is the source of truth for persistent application data.

Each service owns its own database or logical database boundary.

Services must not directly query each other's tables.

Flyway owns schema creation and schema evolution.

Example migration structure:

```text
db/migration/
├── V1__create_users.sql
├── V2__create_refresh_tokens.sql
└── ...
```

Hibernate/JPA maps Java application objects to database data.

Flyway controls schema evolution.

Do not rely on Hibernate automatic schema updates as the production migration strategy.

---

# API and OpenAPI Decisions

The frontend communicates with the BFF.

The BFF communicates with backend services.

Identity Service and Run Service publish OpenAPI contracts.

Planned contract locations:

```text
contracts/
├── identity-api/
│   └── openapi.yml
└── run-api/
    └── openapi.yml
```

OpenAPI Generator generates Java clients for the BFF.

Generated clients should replace repetitive handwritten HTTP client plumbing.

Do not generate:

- Business logic
- Repository implementations
- Persistence entities
- Domain implementations

Server-interface generation is not required for Version 1.

API errors should use a consistent structure.

APIs are treated as contracts.

---

# Testing Strategy

Testing should focus on behavior that provides meaningful confidence.

Use:

- Unit tests for business logic
- Spring Boot integration tests where framework integration matters
- Repository/database integration tests using PostgreSQL-compatible test environments
- Testcontainers when real PostgreSQL behavior needs to be verified
- Security tests for authentication and authorization
- End-to-end tests for the primary user workflow

Avoid testing trivial framework behavior merely to increase test counts.

The critical security test is:

> User A must not be able to read, update, or delete User B's runs.

---

# Docker Strategy

Each deployable application gets its own Docker image.

Docker Compose is used for convenient local execution of the required system components.

Version 1 Docker infrastructure should include only components needed by the actual application.

Do not add containers for unused technology.

---

# Kubernetes and CKAD Strategy

Kubernetes is a required part of Version 1 because deployment experience directly supports CKAD preparation and strengthens the portfolio.

PacesOnline should provide practical experience with:

- Pods through Deployments
- Deployments
- Services
- ConfigMaps
- Secrets
- Liveness probes
- Readiness probes
- Resource requests
- Resource limits
- Ingress
- NetworkPolicies
- Environment-based configuration
- Rolling deployments
- Kustomize where it provides useful environment overlays

The project does not need to implement every CKAD topic.

CKAD-specific exercises should also be practiced independently so that the application does not become bloated merely to cover exam objectives.

Kubernetes manifests should remain understandable and intentionally small.

Do not introduce:

- Service mesh
- Kubernetes operators
- Complex platform abstractions
- Multiple deployment frameworks

unless a future requirement clearly justifies them.

---

# Observability

Version 1 observability is intentionally lightweight.

Use:

- Spring Boot Actuator
- Health endpoint
- Liveness
- Readiness
- Useful application logs
- Basic application metrics where useful

A dedicated Prometheus/Grafana observability stack is not required for Version 1.

---

# CI/CD

Version 1 requires one understandable CI pipeline.

The pipeline should eventually:

- Build the applications
- Run automated tests
- Fail on test/build errors
- Build container images when appropriate

Do not introduce multiple CI/CD systems.

Deployment automation may be added only as needed for the final Kubernetes workflow.

---

# Version 1 Repository Structure

```text
paces-online/
├── PROJECT_CONTEXT.md
├── README.md
├── frontend/
├── bff/
├── services/
│   ├── identity-service/
│   └── run-service/
├── contracts/
│   ├── identity-api/
│   └── run-api/
├── infrastructure/
│   ├── docker/
│   └── kubernetes/
└── docs/
```

Directories should be created when they are actually needed.

Empty placeholder directories are not required.

---

# Delivery Roadmap

## Milestone 0 — Project Foundation

Status: substantially complete.

Includes:

- Project vision
- Initial architecture
- Repository structure
- Identity Service bootstrap
- Project context

---

## Milestone 1 — Identity and Access

Deliver:

- Spring profiles
- Type-safe token configuration
- PostgreSQL
- Flyway
- User persistence
- Registration
- Password hashing
- Login
- Access tokens
- Refresh tokens
- Logout
- Authenticated profile
- Authentication/security tests
- Identity OpenAPI contract
- Identity Service Dockerfile

Avoid unrelated identity-administration features.

---

## Milestone 2 — Run Management

Deliver:

- Run persistence model
- Flyway migrations
- Run creation
- Run retrieval
- Run update
- Run deletion
- Run history
- Date filtering
- Run-type filtering
- Backend pace calculation
- Cross-user authorization
- Automated tests
- Run OpenAPI contract
- Run Service Dockerfile

---

## Milestone 3 — BFF and React

Deliver:

- Generated Java API clients
- BFF integration
- React + TypeScript frontend
- Registration UI
- Login UI
- Run creation/editing UI
- Run-history UI
- Authentication handling
- Consistent error handling

At the end of this milestone, the complete application workflow should work locally.

---

## Milestone 4 — Integration and Local Deployment

Deliver:

- Docker Compose
- PostgreSQL containers
- Full local application startup
- Integration tests
- End-to-end core workflow test
- README instructions

The required workflow must work before moving to optional enhancements.

---

## Milestone 5 — Kubernetes and Portfolio Release

Deliver:

- Kubernetes Deployments
- Kubernetes Services
- ConfigMaps
- Secrets
- Liveness probes
- Readiness probes
- Resource requests and limits
- Ingress
- NetworkPolicies
- Kustomize where useful
- CI pipeline
- Deployment documentation
- Architecture documentation
- Screenshots or demo material
- Final portfolio README

Version 1 is complete when the main workflow is secure, tested, documented, and deployable.

---

# Explicitly Out of Scope for Version 1

The following technologies and features are deferred until after the primary career and CKAD goals have been achieved:

- Redis
- RabbitMQ
- Kafka
- Notification Worker
- MinIO
- S3/object-storage integration
- Run photos
- Email notifications
- Weekly notification jobs
- Complex retry/dead-letter infrastructure
- Administrator role-management workflows
- Account-management administration
- Complex security-audit subsystem
- Weekly/monthly analytics dashboards
- Advanced analytics
- Elasticsearch
- Advanced observability platforms
- Prometheus/Grafana deployment stack
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

These may be revisited after Version 1 and after the current job-search/CKAD priorities.

---

# Development Process

Keep project management lightweight.

Use:

```text
PROJECT_CONTEXT.md
        ↓
GitHub issue
        ↓
implementation
        ↓
tests
        ↓
commit / pull request
```

`PROJECT_CONTEXT.md` records stable project decisions.

The active GitHub issue defines the current scope.

Git commits record implementation progress.

A handoff document is created only when it is genuinely useful for continuity.

Do not create documentation or process artifacts solely because a template exists.

Before implementing a new abstraction, ask:

1. What concrete problem does it solve?
2. Does Spring Boot, React, PostgreSQL, Docker, or Kubernetes already solve it?
3. Is it required for the core workflow?
4. Does it materially support CKAD or the intermediate portfolio goal?

If the answers do not justify the complexity, do not add it.

---

# Quality Standard

Intermediate-level engineering should be demonstrated through correct decisions and finished behavior, not through maximum architecture complexity.

Important qualities include:

- Clear boundaries
- Correct validation
- Secure authentication
- Correct authorization
- Database migrations
- Good API contracts
- Meaningful automated tests
- Externalized configuration
- Useful logging
- Containerization
- Kubernetes deployment
- CI
- Documentation
- Ability to explain architectural tradeoffs

A feature is complete when it works reliably and is adequately tested.

Not every feature requires a new abstraction, design document, or framework.

---

# Current Work

## Completed

Issue #1 — Bootstrap Identity Service

The Identity Service baseline includes:

- Standalone Spring Boot application
- Actuator
- Health endpoint
- Liveness and readiness groups
- Application-context startup test
- Service README
- Successful Maven verification

## Active

Issue #2 — Configure Spring Profiles and Type-Safe Configuration

Issue #2 establishes:

- `application.yml`
- `application-local.yml`
- `application-test.yml`
- `application-prod.yml`
- External profile activation
- `TokenProperties`
- Configuration validation
- Fail-fast startup
- Environment-variable overrides
- Safe secret handling
- Configuration tests
- README documentation

Issue #2 does not include database configuration.

Database configuration begins with the PostgreSQL/Flyway persistence work and will use Spring Boot's standard datasource configuration.

---

# Source of Truth

When sources disagree:

1. An intentionally approved architecture decision supersedes an older decision.
2. `PROJECT_CONTEXT.md` defines current stable project direction.
3. The active GitHub issue defines the current implementation scope.
4. The current Git branch and commits define actual code state.
5. Old handoffs are historical and must not override newer project decisions.
6. Chat memory is advisory and must not override repository state.

The goal is continuity without unnecessary project-management overhead.
