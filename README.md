# MicroVault

Personal finance workspace.

Sprint 1 keeps the existing HTML/CSS/JavaScript frontend and adds a **plain Java + JDBC + PostgreSQL** backend foundation.

This is not a complete production application. There is no Spring, Hibernate, JPA or REST controller layer.

## Project structure

```text
MicroVault/
│
├── frontend/
│   ├── html/          login, register, landing, terms, forgot-password
│   ├── css/           page and shared styles
│   ├── js/            app.js, api.js, sidebar scripts
│   ├── assets/        images and other static files
│   └── pages/         dashboard, transactions, goals, budgets, admin pages
│
├── backend/
│   ├── pom.xml
│   ├── README.md
│   ├── sql/
│   │   ├── 01_create_database.sql
│   │   ├── 02_create_tables.sql
│   │   ├── 03_constraints.sql
│   │   ├── 04_indexes.sql
│   │   ├── 05_seed_data.sql
│   │   └── 06_test_queries.sql
│   └── src/
│       ├── main/java/com/microvault/
│       │   ├── model/
│       │   ├── dto/
│       │   ├── dao/
│       │   ├── daoimpl/
│       │   ├── service/
│       │   ├── serviceimpl/
│       │   ├── business/
│       │   ├── config/
│       │   ├── util/
│       │   └── exception/
│       ├── main/resources/database.properties
│       └── test/java/com/microvault/
│           ├── dao/
│           ├── service/
│           ├── business/
│           └── support/
│
└── README.md
```

Frontend files stay in `frontend/`. Java files stay in `backend/`. They are not mixed.

## Frontend

The existing UI was kept as-is.

| Folder | Contents |
|---|---|
| `frontend/html` | Public pages: landing, login, register, terms, forgot-password |
| `frontend/pages` | Signed-in member and admin pages |
| `frontend/css` | Stylesheets |
| `frontend/js` | Shared JavaScript (`app.js`, `api.js`, sidebar) |
| `frontend/assets` | Images and other static files |

Open the HTML files in a browser to view the current UI. The JavaScript already talks to `/api/...`. Sprint 1 does **not** add that HTTP layer. The backend in this sprint is the Java/JDBC foundation only.

## Backend

Package layout:

```text
com.microvault.model          one POJO per table
com.microvault.dto            transfer objects, no password hashes
com.microvault.dao            interfaces
com.microvault.daoimpl        JDBC PreparedStatement implementations
com.microvault.service        business interfaces
com.microvault.serviceimpl    validation, hashing, DAO coordination
com.microvault.business       GoalBO, BudgetBO, AffordabilityBO, ReportBO, TransactionBO
com.microvault.config         DatabaseConfig
com.microvault.util           DBConnection, PasswordUtil
com.microvault.exception      DatabaseException, ValidationException, UserNotFoundException
```

Layering:

```text
Service interface
    -> DAO interface
        -> DAO implementation
            -> JDBC PreparedStatement
                -> PostgreSQL
```

Services receive the DAO interface through the constructor. They do not contain SQL. DAOs do not contain business rules.

## Entities and tables

| Entity | Table |
|---|---|
| User | users |
| FinanceProfile | finance_profiles |
| Transaction | transactions |
| Goal | goals |
| SavingsEntry | savings_entries |
| Budget | budgets |
| AffordabilityCheck | affordability_checks |
| Notification | notifications |
| Report | reports |
| Feedback | feedback |
| FeedbackHistory | feedback_history |
| News | news |

Every table uses:

- `id UUID PRIMARY KEY`
- `created_at`, `updated_at`
- `is_deleted BOOLEAN DEFAULT FALSE`
- `deleted_at TIMESTAMP`

Foreign keys:

```text
User
  -> FinanceProfile
  -> Transaction
  -> Goal
  -> SavingsEntry
  -> Budget
  -> AffordabilityCheck
  -> Notification
  -> Report
  -> Feedback
  -> News (author)

Feedback
  -> FeedbackHistory
```

## Dependencies

Install these before you compile or run tests:

1. **JDK 17 or newer**
2. **Apache Maven 3.8 or newer**
3. **PostgreSQL 13 or newer** (or access to the shared development database)

Maven downloads the only two libraries the project needs:

- `org.postgresql:postgresql:42.7.4`
- `org.junit.jupiter:junit-jupiter:5.10.2` (test scope)

## Database setup

Default development connection (also stored in `backend/src/main/resources/database.properties`):

| Setting | Value |
|---|---|
| Host | 10.23.240.38 |
| Port | 5432 |
| Database | indr_aug13_smartsavingsandinvestment_dev |
| Username | indr_aug13_smartsavingsandinvestment_dev |
| Password | TCS@123 |

Override with environment variables if needed: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

Run the SQL scripts in order while connected to that database:

```powershell
cd backend
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/01_create_database.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/02_create_tables.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/03_constraints.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/04_indexes.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/05_seed_data.sql
```

You can also paste each file into pgAdmin Query Tool.

## Seeded accounts

Passwords are hashed with PBKDF2 before they are stored.

| Role | Email | Password |
|---|---|---|
| Super admin | supermicrovault@microvault.com | SuperAdmin@123 |
| Admin | admin@microvault.com | Admin@1234 |
| Member | aarav.sharma@example.com | User@1234 |
| Member | priya.patel@example.com | User@1234 |
| Inactive member | rohan.mehta@example.com | User@1234 |

## Compile and test

```powershell
cd backend
mvn -q compile
mvn test
```

- Service and business tests always run. They use in-memory DAO stubs.
- DAO tests use the real PostgreSQL connection. They skip themselves when the database or tables are not ready.
- DAO tests create `junit.*@microvault.test` rows and clean them up with **soft delete**. They never run `DELETE FROM`.

## Password handling

`PasswordUtil` hashes passwords with PBKDF2-HMAC-SHA256 and a random salt.

Stored format:

```text
pbkdf2_sha256$iterations$base64Salt$base64Hash
```

Login compares the typed password with that stored hash. Plain-text passwords are never written to PostgreSQL. User DTOs never include the hash.

## Soft delete

Business records are not physically deleted.

```sql
UPDATE users
SET is_deleted = TRUE,
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ? AND is_deleted = FALSE;
```

Reads use `WHERE is_deleted = FALSE`.

## How to call the backend from Java

```java
UserDAO userDAO = new UserDAOImpl();
UserService userService = new UserServiceImpl(userDAO);

User user = new User();
user.setFullName("Aarav Sharma");
user.setEmail("aarav.sharma@example.com");
user.setPhone("9876543210");

UserDTO created = userService.createUser(user, "User@1234");
UserDTO session = userService.login("aarav.sharma@example.com", "User@1234");
```

Affordability, goal progress, budget usage and report totals are calculated in the `business` package, not in the DAO.

## Common problems

| Symptom | What to check |
|---|---|
| `PostgreSQL JDBC driver not found` | Run `mvn compile` so Maven can download the driver |
| Connection refused | Host, port, VPN or credentials in `database.properties` |
| DAO tests skipped | SQL scripts 02-04 have not been applied to the same database |
| Unique email error | Seed data already contains that email; that is expected |
| Unique budget category error | One active budget per category per user |

## Sprint 1 limits

This sprint does **not** include:

- REST controllers or an HTTP server
- Spring or any other application framework
- Hibernate / JPA
- Hard delete of business rows
- Plain-text password storage
- Integer auto-increment IDs

The frontend remains usable as a static UI. Connecting it to a live API is later work.
