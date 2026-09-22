# MicroVault Backend (Sprint 1)

Plain Java + JDBC + PostgreSQL foundation for MicroVault.

This folder is **not** a web server and **not** a complete production application.
It provides the data layer, business rules, SQL schema and JUnit tests.

For the full manual (install JDK/Maven, create the database, run SQL, compile, and verify):

see **[SETUP.md](SETUP.md)**.

There is no Spring, Hibernate, JPA or REST controller layer.

## What this sprint includes

- One model class per database table
- One DTO per entity (password hashes are never copied into UserDTO)
- One DAO interface and one JDBC implementation per entity
- Service interfaces and implementations for validation and coordination
- Business objects for goal progress, budget usage, affordability and reports
- Central PostgreSQL connection (`DatabaseConfig` + `DBConnection`)
- PBKDF2 password hashing (`PasswordUtil`)
- Soft delete only (`is_deleted`, `deleted_at`)
- UUID primary keys
- SQL scripts and JUnit 5 tests

## Folder layout

```text
backend/
  pom.xml
  README.md
  sql/
    01_create_database.sql
    02_create_tables.sql
    03_constraints.sql
    04_indexes.sql
    05_seed_data.sql
    06_test_queries.sql
  src/main/java/com/microvault/
    model/
    dto/
    dao/
    daoimpl/
    service/
    serviceimpl/
    business/
    config/
    util/
    exception/
  src/main/resources/database.properties
  src/test/java/com/microvault/
    dao/
    service/
    business/
    support/
```

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| JDK | 17 or newer | Compile and run Java |
| Apache Maven | 3.8+ | Build and tests |
| PostgreSQL JDBC | 42.7.4 | Database driver (from Maven) |
| JUnit Jupiter | 5.10.2 | Tests only |
| PostgreSQL server | 13+ | Schema and live DAO tests |

No other libraries are required.

## Database connection

Connection details live in **one** place:

`src/main/resources/database.properties`

```properties
db.url=jdbc:postgresql://10.23.240.38:5432/indr_aug13_smartsavingsandinvestment_dev
db.username=indr_aug13_smartsavingsandinvestment_dev
db.password=TCS@123
db.driver=org.postgresql.Driver
```

Environment variables override the file when set:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

DAO classes never contain credentials. They call `DBConnection.getConnection()`.

## How to create the schema

Connect to PostgreSQL, then run the scripts in order.

```powershell
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/01_create_database.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/02_create_tables.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/03_constraints.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/04_indexes.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/05_seed_data.sql
```

`06_test_queries.sql` is a manual checklist. It does not hard-delete business rows.

## Seeded accounts

Passwords are stored as PBKDF2 hashes.

| Role | Email | Password |
|---|---|---|
| Super admin | supermicrovault@microvault.com | SuperAdmin@123 |
| Admin | admin@microvault.com | Admin@1234 |
| Member | aarav.sharma@example.com | User@1234 |
| Member | priya.patel@example.com | User@1234 |
| Inactive member | rohan.mehta@example.com | User@1234 |

## Compile

From the `backend` folder:

```powershell
mvn -q compile
```

## Run tests

```powershell
mvn test
```

What the tests do:

- **Service tests** and **business tests** use in-memory DAO stubs. They always run and do not need PostgreSQL.
- **DAO tests** open a real JDBC connection. If the database or tables are not ready they are skipped, they do not fail the build.
- DAO tests create unique `junit.*@microvault.test` users and clean up with **soft delete** only.

## How to use the layers

```text
UI / later HTTP layer
        |
   Service interface  (validation, hashing, coordination)
        |
   DAO interface
        |
   DAO implementation  (PreparedStatement JDBC)
        |
   PostgreSQL
```

Example:

```java
UserDAO userDAO = new UserDAOImpl();
UserService userService = new UserServiceImpl(userDAO);

User user = new User();
user.setFullName("Aarav Sharma");
user.setEmail("aarav.sharma@example.com");
user.setPhone("9876543210");

UserDTO created = userService.createUser(user, "User@1234");
UserDTO loggedIn = userService.login("aarav.sharma@example.com", "User@1234");
```

The service hashes the password before the DAO stores it. The DTO never includes the hash.

## Soft delete

Normal delete methods run:

```sql
UPDATE <table>
SET is_deleted = TRUE,
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ? AND is_deleted = FALSE;
```

Normal SELECT methods include `WHERE is_deleted = FALSE`.

## Entities

User, FinanceProfile, Transaction, Goal, SavingsEntry, Budget,
AffordabilityCheck, Notification, Report, Feedback, FeedbackHistory, News.
