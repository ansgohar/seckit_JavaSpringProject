# Quickstart Guide: User Registration Feature

**Feature**: User Registration  
**Branch**: `001-user-registration`  
**Stack**: Java 25, Spring Boot 4, Thymeleaf, Spring Security (BCrypt), Spring Data JPA, PostgreSQL, Flyway

---

## 1. Prerequisites

- **Java**: JDK 25 installed (`java -version`)
- **Maven**: Bundled Maven wrapper (`./mvnw`)
- **Database**: PostgreSQL (or in-memory H2 / embedded PostgreSQL for automated tests)

---

## 2. Running Locally

Start the Spring Boot microservice:

```bash
./mvnw spring-boot:run
```

The application will start on port `8080` by default.

---

## 3. Manual Verification Workflow

### Step 1: Open the Registration Page
Open your web browser and navigate to:
```
http://localhost:8080/register
```
Verify that the form contains:
- Full Name input field
- Email Address input field
- Password input field
- "Register" submission button

---

### Step 2: Validate Successful Registration (Post/Redirect/Get)
1. Fill in the form:
   - **Name**: `Alice Walker`
   - **Email**: `alice@example.com`
   - **Password**: `SecurePass123`
2. Click **Register**.
3. **Verify**:
   - The browser is redirected to `http://localhost:8080/login`.
   - A success alert appears: `"Registration completed successfully! Please log in."`.
   - Refreshing the page does NOT re-submit the form (PRG pattern).

---

### Step 3: Validate Duplicate Email Rejection
1. Return to `http://localhost:8080/register`.
2. Enter the same email with different casing:
   - **Name**: `Alice Clone`
   - **Email**: `ALICE@example.com`
   - **Password**: `AnotherPass456`
3. Click **Register**.
4. **Verify**:
   - The form is redisplayed at `http://localhost:8080/register`.
   - The Name field retains `Alice Clone`.
   - The Email field retains `ALICE@example.com`.
   - The Password field is completely empty.
   - A validation error is displayed next to the email field: `"This email address is already in use."`.

---

### Step 4: Validate Field Validation & Password Clearing
1. Clear all fields and click **Register**.
2. **Verify**:
   - All fields display required validation errors.
   - Enter a password with 5 characters (`abc12`) and click **Register**.
   - Error appears: `"Password must be at least 8 characters"`.
   - Password field is wiped clean.

---

### Step 5: Verify Database Storage & BCrypt Hashing
Inspect the database `users` table:
```sql
SELECT id, name, email, password_hash, status, created_at FROM users;
```
**Verify**:
- `email` is stored in normalized lowercase (`alice@example.com`).
- `password_hash` starts with `$2a$` or `$2b$` and contains zero plain text (`SecurePass123` never appears).
- `status` is `'ACTIVE'`.

---

## 4. Automated Verification Suite

Run all isolated unit tests (Domain models, value objects, and application use cases):
```bash
./mvnw test -Dtest="*UnitTest"
```

Run web slice tests and integration tests (Form binding, validation, PRG redirects, JPA persistence):
```bash
./mvnw verify
```
