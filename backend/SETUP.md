# MicroVault Backend — Manual Setup Guide

This document is the step-by-step manual for running the Sprint 1 backend with PostgreSQL.

The backend is **plain Java + JDBC**. It is not a web server. There is no Spring Boot, no REST API, and no `localhost:8080` page. After setup you compile the Java code, apply SQL to PostgreSQL, then run the smoke check or JUnit tests.

---

## 1. What you will set up

| Piece | What it does |
|---|---|
| JDK 17+ | Compiles and runs the Java code |
| Apache Maven 3.8+ | Downloads the PostgreSQL JDBC driver and runs tests |
| PostgreSQL 13+ | Stores the 12 MicroVault tables |
| SQL scripts in `backend/sql/` | Create tables, constraints, indexes, and seed users |
| `database.properties` | The only place JDBC credentials are stored |

After this setup you can:

1. Connect Java to PostgreSQL
2. Create / read / update / soft-delete users and other entities
3. Log in with hashed passwords
4. Run JUnit tests

---

## 2. Software to install

Install these on your Windows PC before anything else.

### 2.1 JDK 17 or newer

1. Download Oracle JDK or Eclipse Temurin from:
   - https://www.oracle.com/java/technologies/downloads/
   - or https://adoptium.net/
2. Install it.
3. Add Java to `PATH` if the installer did not do it.
4. Open a **new** PowerShell window and check:

```powershell
java -version
javac -version
```

You need Java 17 or higher. Java 24 is fine.

### 2.2 Apache Maven

1. Download Maven from https://maven.apache.org/download.cgi  
   Use the `apache-maven-3.9.x-bin.zip` file.
2. Unzip it, for example to `C:\Apache\maven`.
3. Add `C:\Apache\maven\bin` to the Windows `PATH`.
4. Open a **new** PowerShell window and check:

```powershell
mvn -version
```

Maven must print a version and the same Java you installed.

### 2.3 PostgreSQL client tools

You need at least one of these:

- **psql** (comes with the PostgreSQL installer)
- **pgAdmin 4** (graphical Query Tool)

Download PostgreSQL / pgAdmin from:

https://www.postgresql.org/download/windows/

If you will use the **shared college/dev database** (`10.23.240.38`), you do **not** have to install a full PostgreSQL server on your laptop. You only need `psql` or pgAdmin so you can run the SQL scripts.

If that host is blocked on your network, install PostgreSQL locally and follow **Option B** below.

Check `psql` (optional, only if you installed it):

```powershell
psql --version
```

---

## 3. Open the project

In PowerShell:

```powershell
cd "C:\Users\Vishnu Shriwastav\OneDrive\Desktop\MicroVault\backend"
```

Confirm these folders exist:

```text
backend\
  pom.xml
  SETUP.md
  sql\
    01_create_database.sql
    02_create_tables.sql
    03_constraints.sql
    04_indexes.sql
    05_seed_data.sql
    06_test_queries.sql
  src\main\java\com\microvault\
  src\main\resources\database.properties
```

---

## 4. Choose a database

You have two options. Use **one**.

### Option A — Shared development database (default)

Use this when you can reach the lab/dev server.

| Setting | Value |
|---|---|
| Host | `10.23.240.38` |
| Port | `5432` |
| Database | `indr_aug13_smartsavingsandinvestment_dev` |
| Username | `indr_aug13_smartsavingsandinvestment_dev` |
| Password | `TCS@123` |

This is already written in:

`backend/src/main/resources/database.properties`

Test the network first:

```powershell
Test-NetConnection 10.23.240.38 -Port 5432
```

If `TcpTestSucceeded` is `False`, the host is blocked (VPN, firewall, or you are off the lab network). Use **Option B**.

### Option B — Local PostgreSQL on your machine

Use this when `10.23.240.38` is not reachable.

1. Install PostgreSQL 13 or newer on Windows.
2. Remember the password you set for the `postgres` superuser.
3. Open **SQL Shell (psql)** or pgAdmin.
4. Connect to the default `postgres` database.
5. Create a local database and user:

```sql
CREATE DATABASE microvault;

CREATE USER microvault WITH PASSWORD 'microvault';

GRANT ALL PRIVILEGES ON DATABASE microvault TO microvault;
```

6. Connect to `microvault` and give the user permission to create tables:

```sql
\c microvault
GRANT ALL ON SCHEMA public TO microvault;
```

7. Edit `backend/src/main/resources/database.properties` to:

```properties
db.url=jdbc:postgresql://localhost:5432/microvault?connectTimeout=5
db.username=microvault
db.password=microvault
db.driver=org.postgresql.Driver
```

Do **not** put these credentials in DAO classes. Keep them only in this file.

You can also override the file without editing it:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/microvault?connectTimeout=5"
$env:DB_USERNAME="microvault"
$env:DB_PASSWORD="microvault"
```

---

## 5. Apply the SQL scripts (manual)

Run the scripts **in this order**. Do not skip files.

| Order | File | What it does |
|---|---|---|
| 1 | `sql/01_create_database.sql` | Confirms you are connected to the right database |
| 2 | `sql/02_create_tables.sql` | Creates the 12 tables with UUID primary keys |
| 3 | `sql/03_constraints.sql` | Unique keys, foreign keys, CHECK rules |
| 4 | `sql/04_indexes.sql` | Indexes for email, user_id, dates, status |
| 5 | `sql/05_seed_data.sql` | Demo users, finance profile, sample rows |
| 6 | `sql/06_test_queries.sql` | Optional SELECT / UPDATE checks (no hard DELETE) |

`02_create_tables.sql` drops the 12 tables if they already exist, then creates them again. That is safe for Sprint 1 development. It does **not** drop other tables in the same database.

### 5.1 Using psql (shared database)

```powershell
cd "C:\Users\Vishnu Shriwastav\OneDrive\Desktop\MicroVault\backend"

psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/01_create_database.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/02_create_tables.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/03_constraints.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/04_indexes.sql
psql -h 10.23.240.38 -p 5432 -U indr_aug13_smartsavingsandinvestment_dev -d indr_aug13_smartsavingsandinvestment_dev -f sql/05_seed_data.sql
```

`psql` will ask for the password. Type:

```text
TCS@123
```

### 5.2 Using psql (local database)

```powershell
cd "C:\Users\Vishnu Shriwastav\OneDrive\Desktop\MicroVault\backend"

psql -h localhost -p 5432 -U microvault -d microvault -f sql/01_create_database.sql
psql -h localhost -p 5432 -U microvault -d microvault -f sql/02_create_tables.sql
psql -h localhost -p 5432 -U microvault -d microvault -f sql/03_constraints.sql
psql -h localhost -p 5432 -U microvault -d microvault -f sql/04_indexes.sql
psql -h localhost -p 5432 -U microvault -d microvault -f sql/05_seed_data.sql
```

Password: `microvault` (or the password you chose).

### 5.3 Using pgAdmin (no command line)

1. Open pgAdmin 4.
2. Register a server:
   - **Option A host:** `10.23.240.38`, port `5432`, user `indr_aug13_smartsavingsandinvestment_dev`, password `TCS@123`, database `indr_aug13_smartsavingsandinvestment_dev`
   - **Option B host:** `localhost`, port `5432`, user `microvault`, password `microvault`, database `microvault`
3. Open **Query Tool** on that database.
4. File → Open → select `backend/sql/01_create_database.sql` → Execute (F5).
5. Repeat for `02`, `03`, `04`, then `05`.
6. Optional: run `06_test_queries.sql` to see SELECT results.

If `03_constraints.sql` fails with “constraint already exists”, the script was already applied. That is OK. Re-run from `02_create_tables.sql` if you want a clean schema.

### 5.4 Confirm tables exist

In psql or pgAdmin:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

You should see:

```text
affordability_checks
budgets
feedback
feedback_history
finance_profiles
goals
news
notifications
reports
savings_entries
transactions
users
```

Count seed users:

```sql
SELECT full_name, email, role, status
FROM users
WHERE is_deleted = FALSE
ORDER BY role, full_name;
```

---

## 6. Seeded login accounts

Passwords in PostgreSQL are **PBKDF2 hashes**, not plain text.

| Role | Email | Password |
|---|---|---|
| Super admin | `supermicrovault@microvault.com` | `SuperAdmin@123` |
| Admin | `admin@microvault.com` | `Admin@1234` |
| Member | `aarav.sharma@example.com` | `User@1234` |
| Member | `priya.patel@example.com` | `User@1234` |
| Inactive member | `rohan.mehta@example.com` | `User@1234` |

The inactive member can be loaded from the table, but `UserService.login` rejects inactive accounts.

---

## 7. Confirm Java points at the same database

Open:

`backend/src/main/resources/database.properties`

Shared database (Option A):

```properties
db.url=jdbc:postgresql://10.23.240.38:5432/indr_aug13_smartsavingsandinvestment_dev?connectTimeout=5
db.username=indr_aug13_smartsavingsandinvestment_dev
db.password=TCS@123
db.driver=org.postgresql.Driver
```

Local database (Option B):

```properties
db.url=jdbc:postgresql://localhost:5432/microvault?connectTimeout=5
db.username=microvault
db.password=microvault
db.driver=org.postgresql.Driver
```

The JDBC URL, username, and database name must match the server you used in section 5.

---

## 8. Compile the backend

From `backend/`:

```powershell
cd "C:\Users\Vishnu Shriwastav\OneDrive\Desktop\MicroVault\backend"
mvn -DskipTests compile dependency:copy-dependencies -DoutputDirectory=target/dependency
```

What this does:

- Compiles all Java sources into `target/classes`
- Copies `postgresql-42.7.4.jar` into `target/dependency`

Success looks like:

```text
BUILD SUCCESS
```

If Maven says the JDBC driver cannot be downloaded, check internet access and retry.

---

## 9. Run the backend smoke check

This is the local “is the backend working with the database?” command.

```powershell
cd "C:\Users\Vishnu Shriwastav\OneDrive\Desktop\MicroVault\backend"
java -cp "target/classes;target/dependency/*" com.microvault.VerifyApp
```

`VerifyApp` does four things:

1. Hashes a password with `PasswordUtil`
2. Opens a JDBC connection using `database.properties`
3. Creates a test user through `UserService` (password is hashed before insert)
4. Logs in, then **soft-deletes** that test user (`is_deleted = TRUE`)

It never runs `DELETE FROM`.

### Success

```text
MicroVault Sprint 1 verification
--------------------------------
Password hashing : OK
Database connect : OK
User CRUD        : OK
```

### Failure: database not reachable

```text
Password hashing : OK
Database connect : FAILED
User CRUD        : FAILED
```

Fix:

- Shared DB: join the lab network / VPN, then retry `Test-NetConnection 10.23.240.38 -Port 5432`
- Or switch to Option B (local PostgreSQL) and update `database.properties`
- Confirm username, password, host, and database name

### Failure: tables missing

```text
CRUD error: Unable to create user ...
```

or a PostgreSQL message about `relation "users" does not exist`.

Fix: run SQL scripts `02` through `05` on **the same** database named in `database.properties`.

---

## 10. Run JUnit tests

From `backend/`:

```powershell
mvn test
```

What happens:

| Test group | Needs PostgreSQL? | What it proves |
|---|---|---|
| `service/*` and `business/*` | No | Validation, hashing, goal/budget/affordability math |
| `dao/*` | Yes | Real JDBC insert / select / update / soft-delete |

- If PostgreSQL is down, DAO tests are **skipped**. They do not fail the build.
- If PostgreSQL is up and the tables exist, DAO tests run and clean up with soft delete.
- DAO tests use emails like `junit.<uuid>@microvault.test` so they do not overwrite seed users.

Run only the tests that never need the database:

```powershell
mvn test "-Dtest=UserServiceTest,TransactionServiceTest,GoalServiceTest,BudgetServiceTest,AffordabilityServiceTest,ReportServiceTest,GoalBOTest,BudgetBOTest,AffordabilityBOTest"
```

---

## 11. How to call the backend from Java

There is no HTTP URL to open in a browser in Sprint 1. You use the service layer:

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

Layering:

```text
Your Java code
    -> Service  (validation, password hashing)
        -> DAO interface
            -> DAO implementation (PreparedStatement)
                -> PostgreSQL
```

Do not put SQL in the service. Do not put credentials in the DAO.

---

## 12. Optional SQL checks

After seed data is loaded, you can run `sql/06_test_queries.sql` in pgAdmin, or run:

```sql
SELECT * FROM users WHERE is_deleted = FALSE;
SELECT * FROM finance_profiles WHERE is_deleted = FALSE;
SELECT * FROM transactions WHERE is_deleted = FALSE;
```

Soft-delete pattern (do not use `DELETE FROM` on business rows):

```sql
UPDATE users
SET is_deleted = TRUE,
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND is_deleted = FALSE;
```

---

## 13. Common problems

| What you see | Cause | Fix |
|---|---|---|
| `'java' is not recognized` | JDK not on PATH | Reinstall JDK, open a new PowerShell |
| `'mvn' is not recognized` | Maven not on PATH | Add Maven `bin` to PATH, open a new PowerShell |
| `TcpTestSucceeded : False` | Host `10.23.240.38` blocked | Use local PostgreSQL (Option B) |
| `password authentication failed` | Wrong user/password | Match `database.properties` to the server |
| `database ... does not exist` | Connected to the wrong DB | Create it, or fix the database name |
| `relation "users" does not exist` | Scripts 02–05 not applied | Run the SQL files in order |
| `constraint ... already exists` | Script 03 run twice | Ignore, or re-run from script 02 |
| `unique constraint` on email | Seed user already exists | Expected; script 05 skips existing emails |
| DAO tests skipped | Database not reachable or tables missing | Fix connection, apply SQL, run `mvn test` again |
| VerifyApp CRUD failed after connect OK | Tables or constraints missing | Re-run `02`–`05` |

---

## 14. Quick checklist

1. [ ] `java -version` shows 17+
2. [ ] `mvn -version` works
3. [ ] PostgreSQL is reachable (shared `10.23.240.38` **or** local `localhost`)
4. [ ] `database.properties` matches that server
5. [ ] SQL files `01` → `05` have been executed
6. [ ] `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'` shows 12 tables
7. [ ] `mvn -DskipTests compile dependency:copy-dependencies -DoutputDirectory=target/dependency` succeeds
8. [ ] `java -cp "target/classes;target/dependency/*" com.microvault.VerifyApp` prints all **OK**
9. [ ] `mvn test` has 0 failures

When step 8 prints `Password hashing : OK`, `Database connect : OK`, and `User CRUD : OK`, the backend is working with the database.
