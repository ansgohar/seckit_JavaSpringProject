# Web Form Contract: User Registration

**Protocol**: HTTP / Spring MVC Server-Side Rendering (Thymeleaf)  
**Base Path**: `/`  
**Feature**: User Registration  

---

## 1. Display Registration Form

### Request
```http
GET /register HTTP/1.1
Host: localhost:8080
Accept: text/html
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: text/html;charset=UTF-8

<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
  <!-- Renders form with name, email, and password fields -->
</html>
```

**Model Attributes**:
- `registrationForm`: An empty `RegistrationForm` instance.

---

## 2. Submit Registration Form

### Request
```http
POST /register HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Accept: text/html

name=Jane+Doe&email=jane.doe%40example.com&password=SecretPass123
```

### Parameters
| Name | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `name` | string | Yes | 1..100 chars, non-blank | Full legal or display name |
| `email` | string | Yes | 1..254 chars, valid email format | Normalized to lowercase |
| `password` | string | Yes | 8..128 chars | Plain-text password in transit |

---

### Response 1: Successful Registration (Post/Redirect/Get)
```http
HTTP/1.1 303 See Other
Location: /login
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax
```
**Flash Attributes**:
- `successMessage`: `"Registration completed successfully! Please log in."`

---

### Response 2: Validation Failure
Triggered when required fields are missing, invalid syntax, or password too short.

```http
HTTP/1.1 200 OK
Content-Type: text/html;charset=UTF-8

<!-- Redisplays register.html with: -->
<!-- 1. Input fields populated with submitted name and email -->
<!-- 2. Password field empty -->
<!-- 3. Validation error elements rendered next to invalid fields -->
```

**Field Error Mapping**:
- `name`: `"Name is required"` or `"Name cannot exceed 100 characters"`
- `email`: `"Email address is required"` or `"Please enter a valid email address"`
- `password`: `"Password is required"` or `"Password must be at least 8 characters"`

---

### Response 3: Duplicate Email Address
Triggered when email address is already registered in the system.

```http
HTTP/1.1 200 OK
Content-Type: text/html;charset=UTF-8

<!-- Redisplays register.html with: -->
<!-- 1. Submitted name and email preserved -->
<!-- 2. Password field empty -->
<!-- 3. Error on email field: "This email address is already in use." -->
```

---

### Response 4: Rate Limit Exceeded
Triggered when more than 5 registration submissions originate from the same IP within 60 seconds.

```http
HTTP/1.1 429 Too Many Requests
Content-Type: text/html;charset=UTF-8
Retry-After: 60

<!-- Displays a rate limit warning informing user to wait before submitting again -->
```

---

## 3. Login Landing Page (Redirect Target)

### Request
```http
GET /login HTTP/1.1
Host: localhost:8080
Accept: text/html
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: text/html;charset=UTF-8

<!-- Renders login.html displaying the success alert if successMessage flash attribute is present -->
```
