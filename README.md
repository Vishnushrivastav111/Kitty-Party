# MicroVault

Personal finance workspace. The HTML frontend talks to one API gateway. The gateway forwards each request to a small set of Spring Boot services. All of those services use the same PostgreSQL database.

```text
                    ┌─────────────────┐
                    │    Frontend     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │
                    │     :8080       │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   Auth Service       Finance Service    Dashboard Service
      :8081               :8082               :8083
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                             ▼
                       PostgreSQL
                      Single Database

                    Admin Service :8084
```

There is no RabbitMQ, Kafka, Redis, Eureka, or a separate database per service.

## Services

| Service | Port | Owns |
|---|---|---|
| API Gateway | 8080 | Routing only |
| Auth Service | 8081 | `users`, login, registration, OTP password reset |
| Finance Service | 8082 | `finance_profiles`, `transactions`, `budgets`, `goals`, `savings_entries`, `affordability_checks`, `reports` |
| Dashboard Service | 8083 | No tables. It calls Auth, Finance and Admin and builds the dashboard payload |
| Admin Service | 8084 | `notifications`, `feedback`, `feedback_history`, `news`, plus user administration through Auth |

A service never imports another service's entity or repository classes. When one service needs data that belongs to another, it calls that service over HTTP with Spring `RestClient`.

## Affordability and the suggested plan

Finance Service works this out when you check a purchase:

1. Monthly surplus = monthly income − monthly expenses. If that number is negative, it is treated as zero.
2. Available capacity = surplus + 30% of the member's savings entries.
3. Spend ratio = purchase amount ÷ available capacity.

| Ratio | Verdict | What the plan says |
|---|---|---|
| 30% or less | Comfortably affordable | Buy now and keep the rest for regular bills |
| 60% or less | Affordable with caution | Buy only if it is urgent, otherwise wait for the next pay cycle |
| Up to 100% | Tight — consider delaying | Delay it and put the surplus into savings first |
| Above 100% | Not recommended | Do not buy it. The response includes the shortfall to save |

`POST /api/affordability/check` stores the check and returns `verdict`, `level`, `available`, `suggestion` and `plan`.

Budget usage is the expense total in that category compared with the monthly limit: within budget, close to the limit (80% or more), or over budget. Goal progress is saved amount ÷ target amount.

## Password reset

`POST /api/auth/forgot/send-otp` creates a 6-digit code that expires in 10 minutes.

Set these when you want the code emailed:

```text
MAIL_ENABLED=true
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=noreply@microvault.local
```

When `MAIL_ENABLED` is false, the code is not emailed. The response includes `data.demoOtp` so you can finish the reset on a local machine. `POST /api/auth/forgot/reset` checks the code and stores a new PBKDF2 password hash.

## API Gateway routes

| Path | Service |
|---|---|
| `/api/auth/**`, `/api/users/**` | Auth :8081 |
| `/api/finance/**`, `/api/transactions/**`, `/api/budgets/**`, `/api/goals/**`, `/api/savings/**`, `/api/reports/**`, `/api/affordability/**` | Finance :8082 |
| `/api/dashboard/**`, `/api/bootstrap` | Dashboard :8083 |
| `/api/admin/**`, `/api/feedback/**`, `/api/notifications/**` | Admin :8084 |

The frontend always calls `http://localhost:8080/api`.

## Main endpoints

### Auth

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/session
POST /api/auth/forgot/send-otp
POST /api/auth/forgot/reset
GET  /api/users/lookup
GET  /api/users/{id}
PUT  /api/users/me
PUT  /api/users/me/password
```

Login returns `{ "ok": true, "token": "...", "user": { ... } }`. Later calls send `Authorization: Bearer <token>`.

### Finance

```http
PUT  /api/finance
POST /api/finance/skip
GET  /api/finance/profile/{userId}
GET  /api/finance/profile/{userId}/summary

GET  /api/transactions
POST /api/transactions
GET  /api/transactions/{id}
PUT  /api/transactions/{id}
DELETE /api/transactions/{id}

GET  /api/budgets
POST /api/budgets
PUT  /api/budgets/{id}

GET  /api/goals
POST /api/goals
PUT  /api/goals/{id}

GET  /api/savings
POST /api/savings

POST /api/affordability/check
POST /api/reports/generate
```

### Dashboard

```http
GET /api/bootstrap?email={email}
GET /api/dashboard
GET /api/dashboard/summary
GET /api/dashboard/stats
```

### Admin

```http
GET  /api/admin/users
PUT  /api/admin/users/{id}
DELETE /api/admin/users/{id}
POST /api/admin/admins
GET  /api/admin/feedback
GET  /api/admin/news
POST /api/admin/news
POST /api/feedback
GET  /api/feedback/{id}/history
PUT  /api/notifications/{id}/read
PUT  /api/notifications/read-all
```

Errors look like this:

```json
{
  "timestamp": "2026-10-03T14:00:00",
  "status": 404,
  "message": "Transaction not found",
  "path": "/api/transactions/10"
}
```

Records are soft-deleted (`is_deleted`, `deleted_at`). Normal reads ignore deleted rows.

## Database

One PostgreSQL database. Hibernate does not create or change tables (`ddl-auto=none`).

The scripts in `docs/sql` match the existing tables. Run them only on a new database, in this order:

```text
docs/sql/01_create_database.sql
docs/sql/02_create_tables.sql
docs/sql/03_constraints.sql
docs/sql/04_indexes.sql
docs/sql/05_seed_data.sql
```

`02_create_tables.sql` drops and recreates tables. Do not run it against a database you need to keep.

Connection settings come from the environment. The defaults match the shared development database already used by this project:

```text
DB_URL=jdbc:postgresql://10.23.240.38:5432/indr_aug13_smartsavingsandinvestment_dev?connectTimeout=5
DB_USERNAME=indr_aug13_smartsavingsandinvestment_dev
DB_PASSWORD=...
```

Override `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for any other database. Do not put a production password in source control.

Seeded accounts (passwords are already hashed):

| Role | Email | Password |
|---|---|---|
| Super admin | supermicrovault@microvault.com | SuperAdmin@123 |
| Admin | admin@microvault.com | Admin@1234 |
| Member | aarav.sharma@example.com | User@1234 |
| Member | priya.patel@example.com | User@1234 |

## How to run

Start Auth and Finance before Dashboard and Admin. Start the gateway last, or at any time after the others are up. The gateway only forwards requests.

```bash
cd auth-service
mvn clean test
mvn spring-boot:run
```

```bash
cd finance-service
mvn clean test
mvn spring-boot:run
```

```bash
cd admin-service
mvn clean test
mvn spring-boot:run
```

```bash
cd dashboard-service
mvn clean test
mvn spring-boot:run
```

```bash
cd api-gateway
mvn clean test
mvn spring-boot:run
```

Then open the frontend HTML files in a browser. They call `http://localhost:8080/api`.

## Swagger

Each API service serves Swagger UI at:

```text
http://localhost:8081/swagger-ui/index.html
http://localhost:8082/swagger-ui/index.html
http://localhost:8083/swagger-ui/index.html
http://localhost:8084/swagger-ui/index.html
```

## Tests

Service tests use JUnit 5 and Mockito. Controller tests use `@WebMvcTest` and MockMvc. They do not connect to PostgreSQL.

```bash
cd auth-service && mvn clean test
cd finance-service && mvn clean test
cd dashboard-service && mvn clean test
cd admin-service && mvn clean test
cd api-gateway && mvn clean test
```

## Project layout

```text
MicroVault/
├── frontend/
├── api-gateway/
├── auth-service/
├── finance-service/
├── dashboard-service/
├── admin-service/
├── docs/sql/
└── README.md
```
