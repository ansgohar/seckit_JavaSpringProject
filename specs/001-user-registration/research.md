# Technical Research: User Registration

**Feature**: User Registration  
**Date**: 2026-10-19  
**Status**: Completed  
**Specs Directory**: `specs/001-user-registration`

---

## 1. Presentation Layer: Spring MVC + Thymeleaf SSR & Bean Validation

### Decision
Implement the registration interface as server-side rendered HTML using Spring MVC and Thymeleaf (`spring-boot-starter-thymeleaf`). Input binding is handled via a dedicated form record/class `RegistrationForm` annotated with Jakarta Bean Validation constraints (`@NotBlank`, `@Size`, `@Email`).

### Rationale
- Server-side rendering with Thymeleaf provides zero-JavaScript accessibility, fast initial load, and native integration with Spring MVC's `BindingResult`.
- Using `#fields.hasErrors('fieldName')` and `th:errors="*{fieldName}"` enables precise, accessible field-level error messages next to each input element.
- Model binding automatically preserves submitted values in the form model on validation failure. The controller explicitly sets `form.setPassword("")` before redisplaying the template to satisfy the requirement that password fields are cleared on error.

### Alternatives Considered
- *Pure Single-Page App (SPA) with REST API*: Required full client-side state management and additional build tooling. Rejected because the requirement explicitly requests a Spring MVC web application using Thymeleaf for server-side rendered pages.
- *Vanilla JSP*: Legacy technology lacking modern HTML5 template mode and dialect support in Spring Boot 4.

---

## 2. Navigation Flow: Post/Redirect/Get (PRG) Pattern & Flash Attributes

### Decision
Adopt the classic Post/Redirect/Get (PRG) pattern for form submission handling:
1. `GET /register`: Displays empty registration form.
2. `POST /register`: Validates form.
   - If invalid: Returns `register` view (HTTP 200/400) with `BindingResult` and cleared password.
   - If valid: Executes `RegisterUserUseCase`, sets a flash attribute via `RedirectAttributes.addFlashAttribute("successMessage", "Registration completed successfully! Please log in.")`, and returns `redirect:/login`.
3. `GET /login`: Renders the login page and displays the flash message if present.

### Rationale
- PRG eliminates duplicate form submissions caused by browser refreshes or back-button clicks.
- Spring MVC `RedirectAttributes.addFlashAttribute` stores the message temporarily in the session (or cookie) just long enough to survive the redirect, automatically cleaning it up afterward.

### Alternatives Considered
- *Direct Forward to Login Template on POST*: Causes URL mismatch (`POST /register` showing login screen), breaking bookmarking and allowing duplicate submissions on refresh.

---

## 3. Email Normalization & Dual-Layer Uniqueness Enforcement

### Decision
Normalize email addresses by trimming whitespace and converting to lowercase (`email.trim().toLowerCase(Locale.ROOT)`) within the Domain `Email` value object before any processing. Enforce uniqueness at two distinct layers:
1. **Application Layer**: `RegisterUserUseCase` queries `UserRepositoryPort.existsByEmail(normalizedEmail)`. If `true`, throws domain exception `DuplicateEmailException`, which is caught by the web controller to bind a global/field error to `bindingResult.rejectValue("email", "error.duplicate", "This email address is already in use.")`.
2. **Database Layer**: Unique index `idx_users_email_unique` defined in PostgreSQL via Flyway migration script `V1__create_users_table.sql`. Catches race conditions under concurrent submissions, translated by the infrastructure adapter into `DuplicateEmailException`.

### Rationale
- Application-layer check provides user-friendly validation feedback without relying on database constraint violation exceptions.
- Database unique constraint provides absolute data integrity and prevents race conditions in high-concurrency environments.

### Alternatives Considered
- *Database-only constraint checking*: Relies on parsing `DataIntegrityViolationException`. Violates Clean Architecture by letting DB exceptions dictate presentation error flow.
- *Application-only check*: Susceptible to race conditions during simultaneous registrations with the same email.

---

## 4. Password Security: BCrypt Password Hashing

### Decision
Hash passwords using Spring Security's `BCryptPasswordEncoder` with a default cost factor of 12 (`BCryptPasswordEncoder(12)`).

### Rationale
- Conforms to Constitution Principle VII (Zero-Trust Security & Operational Resilience).
- BCrypt incorporates an automatically generated salt per hash, protecting against rainbow table attacks.
- The cost factor can be calibrated per environment (lower in tests for execution speed; 12 in production for brute-force resistance).
- The `PasswordEncoderPort` interface is defined in the Application layer, with its implementation (`BCryptPasswordEncoderAdapter`) in the Infrastructure layer, keeping Domain and Application free of Spring Security dependencies.

### Alternatives Considered
- *Argon2id*: Stronger memory-hard function, but requires external native libraries or additional dependencies. BCrypt is natively provided by Spring Security and standard across production Spring environments.
- *PBKDF2*: Slower to adapt to GPU cracking compared to BCrypt.

---

## 5. Clean Architecture Layering & Port Boundaries

### Decision
Structure the feature into four explicit layers adhering to Constitution Principle I:
- **Domain Layer (`domain`)**:
  - `User`: Entity containing `UserId`, `Name`, `Email`, `HashedPassword`, `UserStatus`, `Instant createdAt`.
  - `Email`: Value object enforcing normalization (trim + lowercase) and format validation.
  - `HashedPassword`: Value object holding verified BCrypt hash.
  - Domain exceptions: `DuplicateEmailException`, `DomainValidationException`.
- **Application Layer (`application`)**:
  - `RegisterUserCommand`: Record containing raw `name`, `email`, `password`.
  - `RegisterUserResult`: Record containing created `UserId`, `name`, `email`.
  - `RegisterUserUseCase`: Single-method use-case interactor class (`execute(...)`).
  - Ports (`ports`): `UserRepositoryPort`, `PasswordEncoderPort`.
- **Infrastructure Layer (`infrastructure`)**:
  - `UserJpaEntity`: JPA entity mapped to `users` table.
  - `SpringDataUserRepository`: Spring Data JPA interface.
  - `UserRepositoryAdapter`: Implements `UserRepositoryPort`.
  - `BCryptPasswordEncoderAdapter`: Implements `PasswordEncoderPort`.
  - `V1__create_users_table.sql`: Flyway migration script.
- **Presentation Layer (`presentation`)**:
  - `RegistrationForm`: Form backing bean with Jakarta Bean Validation annotations.
  - `RegistrationController`: Spring MVC `@Controller` handling `/register` routes.
  - `LoginController`: Stub `@Controller` handling `/login` landing route for flash message display.
  - `templates/register.html`: Thymeleaf registration template with field error blocks.
  - `templates/login.html`: Thymeleaf login template displaying success flash message.

### Rationale
- Completely isolates core domain logic from web frameworks and database libraries.
- Unit tests can test `User` domain rules and `RegisterUserUseCase` without any Spring container in milliseconds.

---

## 6. Rate Limiting Strategy (Public Registration Endpoint)

### Decision
Implement in-service rate limiting using an in-memory token bucket filter (`RegistrationRateLimitFilter`) configured for 5 requests per client IP per minute, intercepting `POST /register`.

### Rationale
- Satisfies clarification requirement Q4 / FR-012.
- Protects the service from automated registration bots, script injection, and BCrypt CPU exhaustion attacks.
- Returns HTTP 429 Too Many Requests when rate limit is exceeded.
