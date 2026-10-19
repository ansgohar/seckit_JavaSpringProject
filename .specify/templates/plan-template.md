# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 25 (LTS)  
**Primary Dependencies**: Spring Boot 4.1.1, Spring Framework 7.0.9, Spring Security (OAuth2 Resource Server / JWT), Spring Data JPA, Actuator, Micrometer  
**Storage**: PostgreSQL (Production), Embedded PostgreSQL / Testcontainers (Test)  
**Testing**: JUnit 5, Mockito (no Spring context for unit tests), @WebMvcTest, @DataJpaTest, @SpringBootTest  
**Target Platform**: Linux server / Container (JVM 25)  
**Project Type**: REST Microservice (Clean Architecture)  
**Performance Goals**: Bounded latency, N+1 query prevention, explicit thread/connection pool sizing  
**Constraints**: Stateless JWT auth, RFC 9457 Problem Details error responses, Flyway migrations, structured JSON logs  
**Scale/Scope**: Single bounded context microservice

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [ ] **Clean Architecture Strict Layering**: Source code organized into 4 layers: Domain, Application, Infrastructure, Presentation. Inner layers have ZERO knowledge or dependencies on outer layers.
- [ ] **Domain Layer Isolation**: Domain entities, value objects, and domain services contain NO framework annotations (`@Entity`, `@Service`, `@Repository`, `@Autowired`). Invariants validated within domain models.
- [ ] **Single-Method Use Cases**: Every use case in the Application layer is a dedicated class with a single public method (e.g., `execute(...)`).
- [ ] **Presentation Simplicity**: REST controllers only translate HTTP inputs to use-case calls and serialize responses. No business or persistence logic in controllers.
- [ ] **Dependency Injection**: Constructor injection exclusively used. Zero field injection (`@Autowired` on member fields). Dependencies in Domain/Application declared through ports (interfaces).
- [ ] **DTO & Record Discipline**: All request/response DTOs and value objects are Java records. JPA entities are NEVER exposed in API responses or accepted in request bodies. Explicit mappers used (no reflection mappers).
- [ ] **RESTful API Standards**: REST semantics, versioned URL paths (`/api/v1/`), RFC 9457 Problem Details for all error responses via `@RestControllerAdvice`, OpenAPI 3.1 annotations on all endpoints, strict pagination on collection queries.
- [ ] **Persistence & Schema Migrations**: Spring Data JPA used exclusively in Infrastructure. Schema changes managed via Flyway versioned migrations. Lazy loading by default; N+1 queries prevented via fetch joins / entity graphs.
- [ ] **Zero-Trust Security**: Stateless OAuth2 Resource Server validating JWT on every request. Deny-by-default access control. Passwords hashed with BCrypt. Sensitive data redacted from logs/responses.
- [ ] **Resilience**: Explicit connection/read timeouts on outbound calls/DB queries. Circuit breakers (Resilience4j) on external dependencies. Thread pools and connection pools explicitly sized.
- [ ] **Observability & Logging**: Actuator endpoints (`/actuator/health`, `/prometheus`), structured JSON logging to stdout with `traceId`/`spanId` and entity IDs. No `System.out`/`System.err`. Custom Micrometer metrics.
- [ ] **Testing Discipline**: Unit tests for Domain and Application run with JUnit 5 + Mockito without Spring context. Slice tests (`@WebMvcTest`, `@DataJpaTest`) and integration tests (`@SpringBootTest` with embedded PostgreSQL) verify behavior and query counts.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (Clean Architecture layout)

```text
src/main/java/com/gohar/platform/seckit/
├── domain/                  # Entities, Value Objects, Domain Services (zero framework deps)
├── application/             # Use-Case Interactors (1 class per use case), Ports (interfaces)
├── infrastructure/          # Port implementations: JPA Repositories, Adapters, Config
└── presentation/            # REST Controllers, Request/Response Records, ControllerAdvice (RFC 9457)

src/main/resources/
├── db/migration/            # Flyway versioned SQL migrations (V1__...sql)
├── application.yml          # Base configuration
├── application-dev.yml      # Local dev configuration
└── application-prod.yml     # Production configuration

src/test/java/com/gohar/platform/seckit/
├── unit/                    # JUnit 5 & Mockito (no Spring context)
├── slice/                   # @WebMvcTest & @DataJpaTest slice tests
└── integration/             # @SpringBootTest integration tests
```

**Structure Decision**: Clean Architecture four-layer layout as mandated by Constitution Principle I.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
