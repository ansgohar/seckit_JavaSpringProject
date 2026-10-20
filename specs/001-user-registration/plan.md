# Implementation Plan: User Registration

**Branch**: `001-user-registration` | **Date**: 2026-10-19 | **Spec**: [specs/001-user-registration/spec.md](spec.md)  
**Input**: Feature specification from `specs/001-user-registration/spec.md`

---

## Summary

Implement the User Registration feature as a Spring Boot 4 Spring MVC web application utilizing Thymeleaf for server-side rendered pages and Clean Architecture for strict business logic decoupling.

Visitors can navigate to `/register`, enter their name, email address, and password, and submit the registration form. The input is validated using a dedicated `RegistrationForm` backing bean with Jakarta Bean Validation. The domain layer normalizes email addresses to lowercase before uniqueness verification. Email uniqueness is verified at both the application layer (via `UserRepositoryPort.existsByEmail`) and database layer (via a unique PostgreSQL index). Passwords are cryptographically hashed using Spring Security's `BCryptPasswordEncoder`. 

Validation failures redisplay `/register` with field-level error messages while preserving non-sensitive form fields and wiping the password. Successful registrations follow the Post/Redirect/Get (PRG) pattern, redirecting visitors to `/login` with a confirmation flash message.

---

## Technical Context

**Language/Version**: Java 25 (LTS)  
**Primary Dependencies**: Spring Boot 4.1.1, Spring Framework 7.0.9, Spring MVC, Thymeleaf (`spring-boot-starter-thymeleaf`), Spring Security (`BCryptPasswordEncoder`), Jakarta Bean Validation (`spring-boot-starter-validation`), Spring Data JPA, Flyway  
**Storage**: PostgreSQL (Production), H2 (PostgreSQL compatibility mode) / Embedded PostgreSQL / Testcontainers (Test)  
**Testing**: JUnit 5, Mockito (fast unit tests without Spring context), `@WebMvcTest` (MVC slice tests), `@DataJpaTest` (JPA repository slice tests), `@SpringBootTest` (end-to-end integration tests)  
**Target Platform**: Linux server / Docker Container (JVM 25)  
**Project Type**: Spring MVC Web Application & Microservice (Clean Architecture)  
**Performance Goals**: Registration response time < 1.5 seconds, N+1 query prevention, rate-limited to 5 requests/min per IP  
**Constraints**: Post/Redirect/Get navigation, password clearing on failure, BCrypt hashing (cost 12), Flyway versioned migrations, no plain text passwords stored or logged  
**Scale/Scope**: Single bounded context user account domain

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Clean Architecture Strict Layering**: Source code organized into 4 decoupled layers: Domain, Application, Infrastructure, Presentation. Inner layers have ZERO knowledge or dependencies on outer layers.
- [x] **Domain Layer Isolation**: Domain models (`User`, `Email`, `HashedPassword`) contain NO framework annotations (`@Entity`, `@Service`, `@Repository`, `@Autowired`). Invariants validated within models.
- [x] **Single-Method Use Cases**: `RegisterUserUseCase` is a dedicated class with a single public method `RegisterUserResult execute(RegisterUserCommand command)`.
- [x] **Presentation Simplicity**: `RegistrationController` only binds `RegistrationForm`, maps to `RegisterUserCommand`, delegates to `RegisterUserUseCase`, and handles view resolution/redirection. No business logic in controller.
- [x] **Dependency Injection**: Constructor injection exclusively used across all components. No field injection. Dependencies in Domain/Application declared through ports (`UserRepositoryPort`, `PasswordEncoderPort`).
- [x] **DTO & Record Discipline**: Commands (`RegisterUserCommand`) and Results (`RegisterUserResult`) are immutable Java records. JPA entities (`UserJpaEntity`) are strictly encapsulated in Infrastructure.
- [x] **RESTful & Web Standards**: Adheres to standard HTTP semantics, Post/Redirect/Get (PRG) pattern for form submissions, and field-level validation error mapping.
- [x] **Persistence & Schema Migrations**: Spring Data JPA used exclusively in Infrastructure. Database schema managed via Flyway migration `V1__create_users_table.sql` with unique index on `LOWER(email)`.
- [x] **Zero-Trust Security**: Passwords hashed using BCrypt via `PasswordEncoderPort`. Plain-text passwords never stored, exposed in logs, or preserved on form validation redisplay.
- [x] **Resilience & Rate Limiting**: In-service rate limiting filter (`RegistrationRateLimitFilter`) throttles registration attempts to 5 requests/min per IP.
- [x] **Observability & Logging**: Structured logging with contextual fields; passwords and sensitive credentials strictly redacted from log statements.
- [x] **Testing Discipline**: Unit tests for Domain and Application run with JUnit 5 + Mockito without Spring context. Slice tests (`@WebMvcTest`) verify form binding and PRG. Integration tests (`@SpringBootTest`) verify database persistence and unique constraint handling.

---

## Project Structure

### Documentation (`specs/001-user-registration/`)

```text
specs/001-user-registration/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 architectural decisions & trade-offs
├── data-model.md        # Phase 1 domain models, form models & SQL schema
├── quickstart.md        # Phase 1 developer setup & manual verification guide
├── contracts/           # Phase 1 interface contracts
│   └── registration-form.md # Web form endpoints, parameters, and PRG redirects
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── tasks.md             # Phase 2 task decomposition (/speckit-tasks output)
```

### Source Code (`src/`)

```text
src/main/java/com/gohar/platform/seckit/
├── domain/                                     # Layer 1: Core Domain (Zero Framework Deps)
│   ├── model/
│   │   ├── User.java                          # User Aggregate Root
│   │   ├── UserId.java                        # User ID Value Object (UUID)
│   │   ├── Email.java                         # Normalized Email Value Object
│   │   ├── HashedPassword.java                # BCrypt Password Hash Value Object
│   │   └── UserStatus.java                    # ACTIVE status enum
│   └── exception/
│       ├── DuplicateEmailException.java       # Domain exception for email conflict
│       └── DomainValidationException.java     # Domain invariant violation exception
│
├── application/                                # Layer 2: Application Use Cases & Ports
│   ├── usecases/
│   │   └── RegisterUserUseCase.java           # Single-method use case interactor
│   ├── command/
│   │   └── RegisterUserCommand.java           # Immutable input command record
│   ├── result/
│   │   └── RegisterUserResult.java            # Immutable output result record
│   └── ports/
│       ├── UserRepositoryPort.java            # Persistence port interface
│       └── PasswordEncoderPort.java           # Password hashing port interface
│
├── infrastructure/                             # Layer 3: Infrastructure Adapters & Persistence
│   ├── persistence/
│   │   ├── UserJpaEntity.java                 # JPA Entity mapped to 'users' table
│   │   ├── SpringDataUserRepository.java      # Spring Data JPA interface
│   │   └── UserRepositoryAdapter.java         # Implements UserRepositoryPort
│   ├── security/
│   │   ├── BCryptPasswordEncoderAdapter.java  # Implements PasswordEncoderPort
│   │   └── SecurityConfiguration.java         # Spring Security configuration (permitAll for public pages)
│   └── ratelimit/
│       └── RegistrationRateLimitFilter.java   # 5 req/min IP sliding window filter
│
└── presentation/                               # Layer 4: Presentation & Web MVC
    ├── form/
    │   └── RegistrationForm.java              # Dedicated form backing bean with Bean Validation
    └── controller/
        ├── RegistrationController.java        # Handles GET/POST /register (PRG pattern)
        └── LoginController.java               # Handles GET /login (landing page with flash message)

src/main/resources/
├── db/migration/
│   └── V1__create_users_table.sql             # Flyway migration with unique index on LOWER(email)
├── templates/
│   ├── register.html                          # Thymeleaf form with field-level error messages
│   └── login.html                             # Thymeleaf login landing page
├── static/
│   └── css/
│       └── styles.css                         # Clean modern styling for registration & login
└── application.yml                            # Base application configuration

src/test/java/com/gohar/platform/seckit/
├── unit/
│   ├── domain/
│   │   ├── UserTest.java                      # Domain entity invariant unit tests
│   │   └── EmailTest.java                     # Email normalization & regex validation tests
│   └── application/
│       └── RegisterUserUseCaseTest.java       # Isolated use case unit test (Mocked ports, NO Spring)
├── slice/
│   ├── presentation/
│   │   └── RegistrationControllerTest.java    # @WebMvcTest for form validation, password clearing & PRG
│   └── infrastructure/
│       └── SpringDataUserRepositoryTest.java  # @DataJpaTest for queries, unique index & entity mapping
└── integration/
    └── UserRegistrationIntegrationTest.java   # @SpringBootTest end-to-end integration test
```

**Structure Decision**: Clean Architecture four-layer layout as mandated by Constitution Principle I, cleanly decoupling domain invariants, application use cases, persistence adapters, and Thymeleaf web presentation.

---

## Complexity Tracking

> No violations of the Constitution Check gates exist. All design decisions adhere strictly to Clean Architecture, Clean Code standards, and test isolation.

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| *None* | N/A | N/A |
