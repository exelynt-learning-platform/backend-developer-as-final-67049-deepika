# Resource Booking System

A RESTful backend for booking shared resources (rooms, vehicles, equipment), built with **Spring Boot 3 / Java 17**, **Spring Security + JWT**, and **PostgreSQL/MySQL** via JPA/Hibernate.

USERs can browse resources and manage their own reservations. ADMINs have full CRUD control over resources and all reservations.

---

## Tech Stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- JWT (jjwt 0.12) — stateless authentication
- PostgreSQL / MySQL (switchable via Spring profile)
- Bean Validation (jakarta.validation)
- springdoc-openapi (Swagger UI)
- JUnit 5, MockMvc, H2 (in-memory test DB)
- Lombok, Maven

---

## Project Structure

```
src/main/java/com/example/bookingsystem/
├── config/          # Security, OpenAPI, and data-seeding configuration
├── security/        # JWT util, filter, UserDetailsService, entry points
├── controller/       # REST controllers (Auth, Resource, Reservation)
├── service/          # Business logic
├── repository/        # Spring Data JPA repositories
├── entity/           # JPA entities (User, Resource, Reservation) + enums
├── dto/               # Request/response DTOs
├── specification/     # JPA Specification for dynamic reservation filtering
└── exception/          # Custom exceptions + global exception handler
```

---

## Getting Started

### 1. Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 13+ **or** MySQL 8+ (a local instance, or Docker)

### 2. Create the database

**PostgreSQL:**
```sql
CREATE DATABASE booking_system;
```

**MySQL** (auto-created since the JDBC URL includes `createDatabaseIfNotExist=true`):
```bash
# No manual step needed, just make sure the MySQL server is running.
```

### 3. Configure environment variables

Copy `.env.example` to `.env` (or export the variables directly in your shell):

```bash
cp .env.example .env
```

Key variables:

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/booking_system` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `DDL_AUTO` | `update` | Hibernate schema strategy |
| `JWT_SECRET` | *(dev default, override in prod)* | HMAC signing key, **must be ≥ 32 chars** |
| `JWT_EXPIRATION_MS` | `3600000` (1h) | Token lifetime |

### 4. Run the application

**Using PostgreSQL (default profile):**
```bash
mvn spring-boot:run
```

**Using MySQL:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Or build and run the jar directly:
```bash
mvn clean package -DskipTests
java -jar target/resource-booking-system-1.0.0.jar
```

The app starts on `http://localhost:8080` and seeds demo data automatically on first boot (see below).

### 5. Run tests

```bash
mvn test
```

Tests run against an in-memory H2 database (`test` profile) — no external DB required.

---

## Seed Users

The app seeds these accounts on startup if they don't already exist:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `user` | `User@123` | USER |
| `jane` | `Jane@123` | USER |

It also seeds 4 sample resources (2 rooms, 1 vehicle, 1 piece of equipment).

---

## API Documentation

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Postman collection:** [`postman_collection.json`](./postman_collection.json) — import into Postman, run "Login as Admin" / "Login as User" first (they auto-populate `{{adminToken}}` / `{{userToken}}` collection variables via test scripts), then call any other request.

---

## Authentication

```
POST /auth/login
Content-Type: application/json

{ "username": "admin", "password": "Admin@123" }
```

Response:
```json
{
  "token": "eyJhbGciOi...",
  "type": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 3600000
}
```

Use the token on every subsequent request:
```
Authorization: Bearer <token>
```

The user's identity (and role) is always resolved server-side from the JWT — reservation ownership is **never** trusted from the request body.

---

## Endpoints

### Auth
| Method | Path | Access |
|---|---|---|
| POST | `/auth/login` | Public |
| GET | `/auth/me` | Authenticated |

### Resources
| Method | Path | Access |
|---|---|---|
| GET | `/api/resources?page=&size=&sort=` | ADMIN, USER |
| GET | `/api/resources/{id}` | ADMIN, USER |
| POST | `/api/resources` | ADMIN |
| PUT | `/api/resources/{id}` | ADMIN |
| DELETE | `/api/resources/{id}` | ADMIN |

### Reservations
| Method | Path | Access |
|---|---|---|
| POST | `/api/reservations` | ADMIN, USER (creates for self only) |
| GET | `/api/reservations?status=&minPrice=&maxPrice=&page=&size=&sort=` | ADMIN sees all, USER sees own |
| GET | `/api/reservations/{id}` | Owner or ADMIN |
| PUT | `/api/reservations/{id}` | ADMIN (full update incl. status) |
| PATCH | `/api/reservations/{id}/cancel` | Owner or ADMIN |
| DELETE | `/api/reservations/{id}` | ADMIN |

**Filtering & pagination example:**
```
GET /api/reservations?status=CONFIRMED&minPrice=20&maxPrice=100&page=0&size=10&sort=price,desc
```

- `status` — one of `PENDING`, `CONFIRMED`, `CANCELLED`
- `minPrice` / `maxPrice` — decimal bounds (inclusive)
- `page` / `size` — standard Spring Data pagination (0-indexed)
- `sort` — e.g. `sort=startTime,asc` (repeatable for multi-field sort)

---

## Security & Validation Highlights

- Passwords hashed with **BCrypt**.
- Stateless JWT auth (no server-side sessions); `JwtAuthenticationFilter` validates the token on every request.
- RBAC enforced with `@PreAuthorize` at the controller layer, backed by `ROLE_ADMIN` / `ROLE_USER` authorities.
- Reservation ownership is enforced in the service layer — a USER can never read, cancel, or otherwise touch another user's reservation, and the reservation's `user` is always resolved from `Authentication#getName()`, not the request body.
- Request bodies validated with Bean Validation (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@FutureOrPresent`, cross-field `endTime > startTime` check).
- Centralized `@RestControllerAdvice` returns structured JSON errors with correct HTTP status codes (400 validation, 401 auth, 403 authorization, 404 not found, 500 fallback).
- Custom `AuthenticationEntryPoint` / `AccessDeniedHandler` ensure 401/403 responses are JSON, not the default HTML error page.

---

## Example Error Response

```json
{
  "timestamp": "2026-08-30T10:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "fieldErrors": {
    "price": "price must not be negative"
  }
}
```

---

## Switching Databases

The app defaults to PostgreSQL. To use MySQL instead:

1. Activate the `mysql` Spring profile (`SPRING_PROFILES_ACTIVE=mysql` or `-Dspring-boot.run.profiles=mysql`).
2. Point `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` at your MySQL instance (see `.env.example` for a ready-made MySQL block).

Both drivers (`postgresql`, `mysql-connector-j`) are bundled, so no dependency changes are needed.
