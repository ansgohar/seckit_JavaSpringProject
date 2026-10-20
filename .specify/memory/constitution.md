<!--
Sync Impact Report:
- Version change: 0.0.0 (Template) → 1.0.0 (Fully Ratified Constitution)
- Ratification Status: Approved and ratified by project owner on 2026-10-19.
- Project Architecture & Type: Headless REST Microservice adhering strictly to Clean Architecture (Domain, Application, Infrastructure, Presentation) with zero framework coupling in the Domain layer.
- Core Principles Enforced:
  - Principle I: Clean Architecture & Strict Layering (NON-NEGOTIABLE)
  - Principle II: Clean Code Standards & Engineering Craftsmanship
  - Principle III: Microservice Statelessness & Single Bounded Context
  - Principle IV: Dependency Injection & Boundary Discipline
  - Principle V: RESTful API Design, Versioning & Standardized Errors
  - Principle VI: Persistence, Schema Migrations & Data Integrity
  - Principle VII: Zero-Trust Security & Operational Resilience
  - Principle VIII: Observability, Metrics & Structured Logging
  - Principle IX: Comprehensive Testing Discipline & Verification
- Project Discovery Confirmation:
  - All standard defaults unanimously approved:
    - Pure REST Microservice, single bounded context.
    - Synchronous REST for queries; asynchronous messaging for distributed side effects.
    - Clean Architecture (Domain, Application, Infrastructure, Presentation) with dedicated single-method use cases.
    - Headless API service (no UI).
    - PostgreSQL (production) with Flyway migrations; embedded PostgreSQL for database integration tests.
    - Stateless OAuth2 Resource Server with JWT verification and deny-by-default RBAC.
    - Strict test discipline: fast unit tests without Spring context (JUnit 5 + Mockito), slice tests, integration tests with query-count verification.
    - Java 25 idioms, no Lombok, no wildcard imports, methods ≤ 20 lines.
    - Kubernetes deployment target, Prometheus metrics via Micrometer, structured JSON logs via SLF4J/Logback with W3C trace propagation.
- Templates Synchronized:
  - .specify/templates/plan-template.md: ✅ Updated Constitution Check gates & Clean Architecture layout
  - .specify/templates/tasks-template.md: ✅ Updated path conventions & mandatory testing discipline
  - .specify/templates/spec-template.md: ✅ Verified aligned
- Follow-up TODOs: None.
-->

# seckit_JavaSpringProject Constitution

## Core Principles

### Principle I: Clean Architecture & Strict Layering (NON-NEGOTIABLE)

Source code MUST be organized into four explicit, decoupled layers: Domain, Application, Infrastructure, and Presentation (API). The Dependency Rule is absolute: outer layers depend on inner layers, but inner layers MUST NEVER depend on outer layers.

* **Domain Layer**: Contains enterprise entities, value objects, aggregates, and domain services. It MUST have zero dependencies on external frameworks, Spring libraries, or infrastructure code. Framework annotations (e.g., `@Entity`, `@Table`, `@Service`, `@Repository`, `@Component`, `@Autowired`) are strictly forbidden in the Domain layer. Domain invariants MUST be validated inside entities and value objects, throwing domain-specific exceptions on violation.
* **Application Layer**: Contains use-case interactors and application services. It depends exclusively on the Domain layer and defines ports (interfaces) for all external dependencies (persistence, messaging, third-party APIs). Every use case MUST be represented by a dedicated class with a single public method (e.g., `execute(...)`), ensuring explicit, traceable, and single-responsibility business workflows.
* **Infrastructure Layer**: Implements ports defined by the Application layer. Houses Spring Data JPA repositories, database entity mappings (`@Entity`), Flyway migrations, messaging adapters, external HTTP clients, and technical configuration.
* **Presentation (API) Layer**: Contains Spring MVC REST controllers, OpenAPI documentation annotations, request/response DTOs, and global exception handlers (`@RestControllerAdvice`). It depends only on Application layer use cases/ports.
* **Boundary Discipline**: Business logic belongs exclusively in the Domain and Application layers. Controllers MUST only translate HTTP input into use-case executions and serialize output to HTTP responses. Controllers MUST NOT contain business or persistence logic.

### Principle II: Clean Code Standards & Engineering Craftsmanship

Clean code standards apply universally to every class, interface, record, and method across the codebase:

* **Meaningful Names**: Class, method, parameter, and variable names must clearly express domain intent without requiring explanatory comments.
* **Small, Focused Methods**: Each method must do one thing and do it well. Any method exceeding twenty lines of code serves as an immediate signal for decomposition.
* **Single Responsibility Principle (SRP)**: Every class must have one, and only one, reason to change.
* **No Magic Numbers or Strings**: All literal constants, magic numbers, and sentinel strings must be extracted into descriptive `static final` constants or strongly typed enums.
* **Zero Dead Code**: Unused imports, commented-out code blocks, and unreachable branches must be deleted immediately.
* **Avoid Deep Nesting**: Cyclomatic complexity and nesting are limited to at most two levels using guard clauses, early returns, and functional composition.
* **DRY (Don't Repeat Yourself)**: Duplicate logic must be extracted into a shared abstraction before a second use is committed.
* **Intentional Comments**: The code itself must express *what* is done; comments are reserved strictly for non-obvious *why* (business rationale or architectural trade-offs).
* **Boy Scout Rule**: Every engineer and automated agent MUST leave every touched file cleaner than they found it.
* **No Wildcard Imports**: All imports must be explicitly declared; wildcard imports (`import foo.bar.*`) are prohibited.
* **No Lombok**: Rely on Java records, explicit constructors, and IDE/compiler generation for boilerplate to keep source code transparent, debuggable, and reflection-free.
* **Modern Java Idioms**: Leverage Java 25 language features—specifically sealed interfaces, pattern matching for `instanceof` and `switch`, and record patterns—to maximize exhaustiveness checking at compile time.

### Principle III: Microservice Statelessness & Single Bounded Context

The microservice must adhere to production-grade distributed systems design principles:

* **Strict Statelessness**: The service must maintain zero server-side session state. All application state lives in the persistence store (PostgreSQL) or within cryptographically verified client tokens (JWT). Any instance must be disposable and replaceable without session loss.
* **Single Bounded Context**: The service encompasses exactly one bounded domain context. It must never absorb responsibilities, models, or data tables outside its domain boundary.
* **Communication Semantics**: Synchronous REST/HTTP is reserved for read queries and immediate command invocations; cross-service side effects, event propagation, and eventual consistency must be executed asynchronously via events or messaging brokers.
* **Idempotency**: All mutating operations (POST, PUT, PATCH, DELETE) must be designed to be idempotent wherever technically feasible, ensuring safe client retries in unreliable network conditions.
* **12-Factor App Discipline**: Externalize all environment configuration, treat backing services (databases, caches, brokers) as attached resources, and maintain dev/prod parity. Environment-specific credentials and URLs must never be committed to source code.

### Principle IV: Dependency Injection & Boundary Discipline

Dependency injection and boundary data structures must adhere to strict decoupling:

* **Constructor Injection Exclusively**: All Spring beans must declare dependencies via final member fields and constructor injection. Field injection (`@Autowired` on member fields) is strictly prohibited.
* **Interface-Driven Ports**: For external dependencies, Domain and Application layers MUST use interfaces (ports), never concrete infrastructure implementations. They may use concrete domain types.
* **Transactional Proxy Calls**: Methods annotated with `@Transactional` MUST be called through a Spring-managed proxy. If another method in the same class needs to call one, move the transactional method to a separate `@Service` and inject that service into the caller.
* **Java Records for DTOs**: All request DTOs, response DTOs, query parameters, and value objects must be declared as immutable Java records.
* **Entity Encapsulation**: JPA entities (`@Entity`) must NEVER be exposed in API responses or accepted in API requests under any circumstances.
* **Explicit Boundary Mapping**: Mapping between Presentation DTOs, Application models, and Infrastructure JPA entities must be performed via explicit mapper classes or static factory methods. Reflection-based mapping frameworks (e.g., ModelMapper, Dozer) are prohibited.

### Principle V: RESTful API Design, Versioning & Standardized Errors

Public and internal HTTP APIs must provide predictable, robust, and self-documenting contracts:

* **REST Semantics**: Follow standard HTTP methods (GET for safe reads, POST for resource creation/action triggers, PUT for complete replacements, PATCH for partial updates, DELETE for resource deletion) and standard status codes (200, 201, 204, 400, 401, 403, 404, 409, 422, 500).
* **URL Path Versioning**: All endpoints must be versioned in the URL path (e.g., `/api/v1/resources`). Breaking changes to an existing version are strictly forbidden; new versions require a new versioned route.
* **RFC 9457 Problem Details**: All error responses must strictly conform to RFC 9457 (`application/problem+json`). A centralized `@RestControllerAdvice` translates domain and validation exceptions into standardized Problem Details. Stack traces, internal class names, database exceptions, and framework internals must never be returned to the client.
* **Inbound Payload Validation**: All inbound payloads must be validated at the Presentation boundary using Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@Size`, etc.). Invariant violations detected in the Domain layer throw domain exceptions that map cleanly to 4xx Problem Details.
* **OpenAPI 3.1 Documentation**: Every endpoint must be documented with Springdoc OpenAPI 3.1 annotations. API documentation must be continuously generated from code and kept synchronized.
* **Strict Pagination**: All collection endpoints must enforce pagination (cursor-based or page/size offset) with explicit, bounded page-size limits to protect system resources.

### Principle VI: Persistence, Schema Migrations & Data Integrity

Data access must protect data integrity, prevent performance degradation, and manage schema evolution:

* **Encapsulated Data Access**: Spring Data JPA repositories and entity definitions belong strictly within the Infrastructure layer.
* **Flyway Versioned Migrations**: All schema creation, modification, and reference data must be managed exclusively through versioned Flyway migration scripts (`V{version}__{description}.sql`). Modifying or deleting an already-applied migration script is strictly forbidden. Integration tests MUST run every Flyway migration script against embedded PostgreSQL and fail if any migration cannot be applied.
* **N+1 Query Prevention**: Lazy loading is mandatory by default. Eager loading is prohibited unless justified by profiling. Queries loading associations must use explicit fetch joins (`JOIN FETCH`) or JPA Entity Graphs (`@EntityGraph`). Query counts must be asserted in integration tests.
* **Database Parity**: PostgreSQL is the standard production database. Database-related integration tests MUST use an embedded PostgreSQL package to guarantee schema and dialect fidelity.
* **Caching Governance**: Caching (e.g., Spring Cache with Redis) may only be introduced when supported by documented, measurable performance requirements, accompanied by an explicit, automated cache invalidation strategy. Redis-backed caching and Redis operations MUST use Spring Data Redis; use `RedisTemplate` or `StringRedisTemplate` for explicit operations. Integration tests for Redis-backed caching MUST use an embedded Redis package.

### Principle VII: Zero-Trust Security & Operational Resilience

Security and operational resilience must be engineered into every layer from inception:

* **Stateless OAuth2 Resource Server**: Spring Security must be configured as a stateless OAuth2 Resource Server. Protected endpoints MUST validate JWT signatures, expirations, and issuers before processing claims. Public endpoints and webhooks exempt from JWT authentication MUST be explicitly listed; webhooks MUST verify the provider's signature or another documented authentication mechanism.
* **Deny-by-Default Authorization**: Access control is deny-by-default. Endpoints require explicit authorization rules based on authenticated scopes, roles, or authorities.
* **HTTP Security Headers**: Spring Security must enforce standard security headers, including HTTP Strict Transport Security (HSTS), X-Content-Type-Options (nosniff), X-Frame-Options (DENY), and Content Security Policy.
* **Sensitive Data Protection**: Passwords must be hashed using BCrypt with an approved cost factor. Sensitive values (tokens, credentials, PII, payment details) must never be written to logs, exposed in error bodies, or included in URL query parameters.
* **Timeouts & Circuit Breakers**: All outbound calls (HTTP clients, database queries, external integrations) must have explicit, non-zero connection and read timeouts. External downstream calls must be protected by Resilience4j circuit breakers, rate limiters, and retry policies.
* **Thread Safety & Pool Configuration**: All Spring components must be stateless and thread-safe. Thread pools and database connection pools (HikariCP) must be sized explicitly; unbounded defaults are prohibited in production.

### Principle VIII: Observability, Metrics & Structured Logging

The service must provide comprehensive observability out-of-the-box for production telemetry:

* **Actuator Endpoints**: Spring Boot Actuator must expose `/actuator/health`, `/actuator/info`, and `/actuator/prometheus`. Management endpoints must be secured and isolated from public business traffic.
* **Structured JSON Logging**: Logs must be emitted to standard output in structured JSON format using SLF4J and Logback (configured with `logstash-logback-encoder`). Direct calls to `System.out.println()` and `System.err.println()` are strictly banned.
* **Distributed Tracing**: HTTP requests and messages between services MUST propagate W3C Trace Context via Micrometer Tracing. Logs emitted while a trace is active MUST include `traceId` and `spanId` in MDC, plus relevant non-sensitive business identifiers when available.
* **Metric Instrumentation**: Business-critical events, use-case invocations, and error rates must be instrumented as custom Micrometer metrics (Counters, Timers, Gauges) published to Prometheus.
* **Log Level Hygiene**: DEBUG for fine-grained diagnostic troubleshooting, INFO for significant lifecycle and business milestones, WARN for handled anomalies or degradations, and ERROR for system failures requiring developer attention.

### Principle IX: Comprehensive Testing Discipline & Verification

Testing must guarantee architectural compliance, functional correctness, and regression prevention:

* **Mandatory Task Tests & Coverage**: Every task that changes production code MUST include both unit and integration tests for the changed behavior. Together, these tests MUST achieve 100% JaCoCo line and branch coverage of new or changed production code.
* **Fast, Isolated Unit Tests**: All unit tests for Domain entities/value objects and Application use cases must be written with JUnit 5 and Mockito and MUST execute without initializing a Spring ApplicationContext.
* **Exhaustive Use-Case Coverage**: Every Application use case must have dedicated unit tests covering the happy path, boundary edge cases, validation rejections, and domain failure scenarios.
* **Targeted Slice Tests**: Presentation layer components must be tested using `@WebMvcTest` with mocked use cases. Repository queries, projections, and entity mappings must be tested using `@DataJpaTest`.
* **Database-Backed Integration Tests**: End-to-end user workflows and cross-layer integration must be verified using `@SpringBootTest` configured with the `test` profile and an embedded PostgreSQL database. Integration tests must verify query counts to guard against N+1 regressions.
* **Deterministic Test Execution**: Tests must be completely isolated, self-contained, and deterministic. Shared mutable state between test executions is prohibited.

---

## Technology Stack Standards

| Component | Standard Selection | Governance & Version Rule |
| :--- | :--- | :--- |
| **Language** | Java 25 (LTS) | Modern features (records, sealed types, pattern matching); no preview features in prod. |
| **Framework** | Spring Boot 4.1.1 | Spring Framework 7.0.9 (transitively managed); strict autoconfiguration audit. |
| **Build Tool** | Maven (`mvnw`) | Maven Wrapper committed; strict dependency management via Spring Boot Parent POM. |
| **Persistence** | Spring Data JPA / Hibernate | Restricted exclusively to the Infrastructure layer; explicit fetch queries. |
| **Database** | PostgreSQL (Production) | Embedded PostgreSQL for database integration tests only. |
| **Cache** | Spring Data Redis | Use `RedisTemplate` or `StringRedisTemplate` for explicit Redis operations; use embedded Redis in cache integration tests. |
| **Migrations** | Flyway | Versioned migrations (`V{version}__{description}.sql`) only; immutability enforced. |
| **Security** | Spring Security OAuth2 + Keycloak | Keycloak is the identity provider; stateless Resource Server verifies JWTs on protected endpoints. |
| **Observability** | Actuator + Micrometer | Prometheus registry for metrics; W3C distributed tracing context propagation. |
| **Logging** | SLF4J + Logback | Structured JSON logging to stdout via `logstash-logback-encoder`. |
| **Documentation** | OpenAPI 3.1 (Springdoc) | Generated directly from code annotations; kept in sync with every release. |
| **Resilience** | Resilience4j | Circuit breakers, retries, and rate limiters on all external integrations. |
| **Testing** | JUnit 5 + Mockito + AssertJ + JaCoCo | Isolated unit tests without Spring context, slice tests, and integration tests; coverage follows Principle IX. |

---

## Environment & Configuration Management

* **Configuration Externalization**: All configuration must strictly adhere to 12-Factor App principles. Configuration that varies across environments (database credentials, client secrets, external service URLs) must be injected via environment variables or a secure secrets manager (e.g., HashiCorp Vault, AWS Secrets Manager).
* **Profile Architecture**:
  * `application.yml`: Defines base defaults, port bindings, Actuator configurations, and common settings.
  * `application-local.yml`: Configured for developer machines with PostgreSQL, local service endpoints, and developer-friendly logging.
  * `application-dev.yml`: Configured for the AWS development environment with PostgreSQL and developer-friendly logging.
  * `application-test.yml`: Configured for the AWS test environment. Automated integration tests use the `test` profile and connect to embedded PostgreSQL for database tests and embedded Redis for cache tests.
  * `application-stage.yml`: Configured for the AWS staging environment with production-like integrations and externalized secrets.
  * `application-prod.yml`: Configured for the AWS production environment with hardened security, pool sizing, and externalized secrets.
* **Zero Secrets in VCS**: Passwords, private keys, API tokens, and production connection strings must NEVER be committed to version control under any circumstances.

---

## Development Workflow & Quality Gates

Every feature, enhancement, and bugfix must satisfy the following automated quality gates before merging into the main branch:

1. **Clean Architecture Check**: Domain layer must have zero dependencies on Spring or Infrastructure. Presentation controllers must not contain business logic.
2. **Clean Code & Linting Gate**: Code must be free of wildcard imports, Lombok annotations, dead code, and methods exceeding 20 lines.
3. **Automated Test Gate**: All unit, slice, and integration tests MUST pass. Every production-code task MUST include unit and integration tests, with 100% JaCoCo line and branch coverage of new or changed production code.
4. **Security & Vulnerability Audit**: Dependency check must report zero high or critical CVEs. No hard-coded credentials or unredacted PII in logs.
5. **Database Migration Verification**: All Flyway migrations MUST be forward-compatible and non-destructive. Integration tests MUST apply every migration to a clean embedded PostgreSQL database successfully.
6. **OpenAPI Documentation Sync**: All new and modified endpoints must include complete OpenAPI 3.1 annotations.
7. **SonarQube Quality Gate**: Every pull request MUST pass the configured SonarQube quality gate before merge.
8. **Sonar Refactoring Verification**: Before each small Sonar refactoring, relevant unit and integration tests MUST be in place and pass. The same tests MUST pass again after the edit. Commit each refactoring separately with the Sonar rule ID and a short description in the commit message.

---

## Governance

* **Precedence**: This constitution is the supreme architectural authority for `seckit_JavaSpringProject`. In any conflict between existing code patterns, documentation, code reviews, and this constitution, the rules in this document supersede all other practices.
* **Amendment Procedure**: Amendments to this constitution must be proposed via an architectural RFC (Request for Comments) or formal Pull Request. The proposal must document: (1) The context and rationale for change, (2) The specific principles affected, (3) The impact on existing code and required migration strategy, and (4) Team review and unanimous consensus.
* **Semantic Versioning Policy**: This constitution is versioned using Semantic Versioning (`MAJOR.MINOR.PATCH`):
  * **MAJOR**: Backward-incompatible governance changes, architectural paradigm shifts, layer redefinitions, or removal of core constraints.
  * **MINOR**: Addition of new principles, new quality gates, or materially expanded architectural guidance.
  * **PATCH**: Phrasing clarifications, typo corrections, reference updates, and non-semantic refinements.
* **Compliance Auditing**: Every Pull Request must include a Constitution Compliance check verifying that new code strictly complies with all principles defined herein.

**Version**: 2.0.0 | **Ratified**: 2026-10-19 | **Last Amended**: 2026-10-20
