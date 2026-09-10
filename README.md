# 🚀 Enterprise Resource Booking REST API

A production-grade, highly optimized RESTful backend service built with **Java 17+**, **Spring Boot 3.x**, **Spring Data JPA**, **Spring Security (Stateless JWT)**, and **MySQL**. It provides robust resource management, double-booking prevention, role-based access control (RBAC), dynamic multi-criteria search/filtering with JPA Specifications, and interactive OpenAPI 3 documentation.

---

## 📑 Table of Contents
- [Architecture & Design Principles](#-architecture--design-principles)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Database Schema & Performance Indexing](#-database-schema--performance-indexing)
- [Security & Authentication Model](#-security--authentication-model)
- [Environment Variables & Configuration](#-environment-variables--configuration)
- [Getting Started](#-getting-started)
- [Default Seeded Accounts](#-default-seeded-accounts)
- [API Documentation & cURL Examples](#-api-documentation--curl-examples)
- [Running Automated Tests & Code Coverage](#-running-automated-tests--code-coverage)

---

## 🏗 Architecture & Design Principles

The application follows clean layered architecture and domain-driven design principles:

```
com.system.booking
├── config          # Security, OpenAPI Swagger, HikariCP, and Startup Seeders
├── controller      # REST API Controllers (Auth, Resource, Reservation)
├── dto             # Strongly-typed Request and Response DTOs
│   ├── request
│   └── response
├── exception       # Global Exception Handling & Domain Exception Models
├── model           # JPA Entities and Business Enums
│   ├── entity
│   └── enums
├── repository      # Spring Data JPA Repositories with EntityGraphs
├── security        # JWT Engine, Filter Chain, Handlers, and UserDetails
│   ├── handler
│   ├── jwt
│   └── service
├── service         # Transactional Business Logic & Identity Context Resolution
└── specification   # Dynamic JPA Criteria Predicates for Multi-Field Filtering
```

---

## ✨ Key Features

1. **Strict Identity Context Binding (Anti-Spoofing)**:
   - When creating or cancelling a reservation, user identity is extracted strictly from `SecurityContextHolder.getContext().getAuthentication().getName()`. Identity cannot be spoofed via the request body.
2. **Double-Booking & Conflict Protection (409 Conflict)**:
   - Overlap queries (`startTime < newEndTime AND endTime > newStartTime`) guarantee that no two active bookings collide on the same resource.
3. **Role-Based Access Control (RBAC)**:
   - **`ROLE_ADMIN`**: Full CRUD operations on resources; ability to view, filter, and modify status on all reservations system-wide.
   - **`ROLE_USER`**: Browse available resources; create bookings; view and cancel strictly their own bookings.
4. **JPA Specification Filtering, Pagination & Sorting**:
   - Filter reservations dynamically across `status`, `minPrice`, and `maxPrice`.
   - Normal users automatically have their user ownership predicate appended to ensure zero cross-tenant data leakage.
   - Full support for `page`, `size`, `sort`.
5. **High-Performance Database Engine**:
   - `open-in-view=false` avoids keeping database connections open during HTTP response rendering.
   - HikariCP connection pool tuned (`maximum-pool-size=20`, `minimum-idle=10`).
   - `@EntityGraph(attributePaths = {"user", "resource"})` eliminates N+1 query overhead.
6. **OpenAPI 3 / Swagger Documentation**:
   - Interactive Swagger UI with embedded Bearer JWT authorization at `/swagger-ui.html`.

---

## 🛠 Tech Stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.2+
- **Security**: Spring Security 6 (Stateless JWT via `io.jsonwebtoken:jjwt:0.12.5`, BCrypt strength 12)
- **Persistence**: Spring Data JPA / Hibernate 6
- **Database**: MySQL 8.x (H2 in-memory configured for test profiles)
- **Connection Pool**: HikariCP
- **Validation**: Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Future`, `@Size`)
- **Documentation**: SpringDoc OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui:2.4.0`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test, JaCoCo

---

## 🗄 Database Schema & Performance Indexing

### 1. `users` Table
| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| `username` | VARCHAR(255) | NOT NULL, UNIQUE | User login handle |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hashed password (strength 12) |
| `role` | VARCHAR(50) | NOT NULL | `ROLE_USER` or `ROLE_ADMIN` |

### 2. `resources` Table
| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique resource identifier |
| `name` | VARCHAR(255) | NOT NULL | Resource display name |
| `description` | VARCHAR(255) | NULLABLE | Detailed description of the resource |
| `type` | VARCHAR(100) | NOT NULL | Resource classification (e.g. `CONFERENCE_ROOM`, `EQUIPMENT`) |

### 3. `reservations` Table
| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique reservation ID |
| `user_id` | BIGINT | NOT NULL, FK -> users(id) | Associated user |
| `resource_id` | BIGINT | NOT NULL, FK -> resources(id) | Associated resource |
| `start_time` | DATETIME(6) | NOT NULL | Booking start time |
| `end_time` | DATETIME(6) | NOT NULL | Booking end time |
| `price` | DECIMAL(10,2) | NOT NULL | Total reservation price |
| `status` | VARCHAR(50) | NOT NULL | `PENDING`, `CONFIRMED`, `CANCELLED` |

### Composite & Single Database Indexes
- `idx_reservation_user_id` on `user_id`: Accelerates user-specific reservation queries.
- `idx_reservation_status_price` on `(status, price)`: Accelerates multi-parameter filtered searches.
- `idx_reservation_start_end_time` on `(start_time, end_time)`: Optimizes overlap collision checks.

---

## 🔒 Security & Authentication Model

- **Password Hashing**: Passwords are encrypted with `BCryptPasswordEncoder(12)`. Raw passwords are never stored, logged, or exposed in DTO responses.
- **JWT Authentication**: Tokens are signed using HMAC-SHA256 (`Keys.hmacShaKeyFor`) with configurable expiration (default 24 hours).
- **Error Responses**:
  - `401 Unauthorized`: Formatted structured JSON response by `JwtAuthenticationEntryPoint`.
  - `403 Forbidden`: Formatted structured JSON response by `CustomAccessDeniedHandler`.
  - `409 Conflict`: Formatted structured JSON response by `GlobalExceptionHandler` on double-booking attempts.

---

## ⚙️ Environment Variables & Configuration

The application reads secrets and configurations from environment variables with safe defaults:

| Property / Env Var | Default Value | Description |
|:---|:---|:---|
| `DB_URL` | `jdbc:mysql://localhost:3306/booking_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048&useServerPrepStmts=true` | JDBC Database Connection URL |
| `DB_USERNAME` | `root` | Database Username |
| `DB_PASSWORD` | `root` | Database Password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 256-bit Secret Key for signing JWTs |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifespan in milliseconds |

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+**
- **Maven 3.8+** (or use included `./mvnw` / `.\mvnw.cmd`)
- **MySQL 8+** (or default test container / local instance)

### 1. Clone the Repository
```bash
git clone https://github.com/exelynt-learning-platform/backend-developer-as-final-81267-samir.git
cd backend-developer-as-final-81267-samir
```

### 2. Build the Application
```bash
# On Linux / macOS:
./mvnw clean package -DskipTests

# On Windows PowerShell:
.\mvnw.cmd clean package -DskipTests
```

### 3. Run the Application
```bash
# On Linux / macOS:
./mvnw spring-boot:run

# On Windows PowerShell:
.\mvnw.cmd spring-boot:run
```

The application will start on `http://localhost:8080`.
Access Interactive Swagger UI at: **`http://localhost:8080/swagger-ui.html`**

---

## 👥 Default Seeded Accounts

The application automatically seeds test credentials on startup via `DataInitializer`:

| Username | Password | Role | Permissions |
|:---|:---|:---|:---|
| `admin` | `admin123` | `ROLE_ADMIN` | Full CRUD on resources, manage all system reservations |
| `john_doe` | `user123` | `ROLE_USER` | Browse resources, create & manage own reservations |

---

## 📡 API Documentation & cURL Examples

### 1. Authentication Endpoints

#### Register a New Account (`POST /auth/register`)
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alex_smith",
    "password": "password123",
    "role": "ROLE_USER"
  }'
```

#### Login (`POST /auth/login`)
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```
*Response:*
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIs...",
  "type": "Bearer",
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

---

### 2. Resource Management (`/api/resources`)

#### Get All Resources (USER / ADMIN)
```bash
curl -X GET http://localhost:8080/api/resources \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

#### Create a Resource (ADMIN ONLY)
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Innovation Lab Boardroom",
    "description": "Equipped with dual 85-inch 4K displays and Cisco video conferencing",
    "type": "CONFERENCE_ROOM"
  }'
```

#### Update a Resource (ADMIN ONLY)
```bash
curl -X PUT http://localhost:8080/api/resources/1 \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Executive Conference Room A (Updated)",
    "description": "Updated room description with high speed WiFi",
    "type": "CONFERENCE_ROOM"
  }'
```

#### Delete a Resource (ADMIN ONLY)
```bash
curl -X DELETE http://localhost:8080/api/resources/1 \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

---

### 3. Reservation Management (`/api/reservations`)

#### Create a Reservation (USER / ADMIN)
*Identity is extracted strictly from the JWT token:*
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <USER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-10-15T10:00:00",
    "endTime": "2026-10-15T12:00:00",
    "price": 120.00
  }'
```

#### Query & Filter Reservations with Pagination (USER / ADMIN)
*Users see only their own bookings; Admins view all bookings across all users:*
```bash
curl -X GET "http://localhost:8080/api/reservations?status=CONFIRMED&minPrice=50.00&maxPrice=300.00&page=0&size=10&sort=startTime,desc" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

#### Get Reservation By ID (Owner or Admin)
```bash
curl -X GET http://localhost:8080/api/reservations/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

#### Cancel Reservation (Owner or Admin)
```bash
curl -X PATCH http://localhost:8080/api/reservations/1/cancel \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

#### Update Reservation Status (ADMIN ONLY)
```bash
curl -X PATCH http://localhost:8080/api/reservations/1/status \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

---

## 🧪 Running Automated Tests & Code Coverage

The test suite includes comprehensive Mockito unit tests, security authorization tests, and exception handling tests configured with an in-memory H2 database:

```bash
# On Linux / macOS:
./mvnw test

# On Windows PowerShell:
.\mvnw.cmd test
```

### Generating JaCoCo Coverage Report
```bash
.\mvnw.cmd clean test jacoco:report
```
View the HTML coverage report at: `target/site/jacoco/index.html`.
