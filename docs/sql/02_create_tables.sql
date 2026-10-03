-- ============================================================
-- MicroVault - 02_create_tables.sql
-- One table per business entity. UUID primary keys, soft delete columns.
-- Run this script while connected to the MicroVault database.
-- ============================================================

-- Dropping is safe to re-run during Sprint 1 development.
-- Child tables are dropped before parent tables.
DROP TABLE IF EXISTS feedback_history;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS news;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS affordability_checks;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS savings_entries;
DROP TABLE IF EXISTS goals;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS finance_profiles;
DROP TABLE IF EXISTS users;


-- ------------------------------------------------------------
-- 1. users
-- ------------------------------------------------------------
CREATE TABLE users (
    id              UUID         PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'user',
    status          VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);


-- ------------------------------------------------------------
-- 2. finance_profiles (one active profile per user)
-- ------------------------------------------------------------
CREATE TABLE finance_profiles (
    id                 UUID          PRIMARY KEY,
    user_id            UUID          NOT NULL,
    income_source      VARCHAR(50),
    monthly_income     NUMERIC(14,2) NOT NULL DEFAULT 0,
    pay_cycle          VARCHAR(30),
    monthly_expenses   NUMERIC(14,2) NOT NULL DEFAULT 0,
    expense_categories TEXT,
    has_loan           BOOLEAN       NOT NULL DEFAULT FALSE,
    loan_type          VARCHAR(50),
    loan_amount        NUMERIC(14,2) NOT NULL DEFAULT 0,
    monthly_emi        NUMERIC(14,2) NOT NULL DEFAULT 0,
    emi_start_date     DATE,
    current_savings    NUMERIC(14,2) NOT NULL DEFAULT 0,
    savings_type       VARCHAR(50),
    investments        NUMERIC(14,2) NOT NULL DEFAULT 0,
    investment_types   TEXT,
    monthly_budget     NUMERIC(14,2) NOT NULL DEFAULT 0,
    budget_style       VARCHAR(50),
    setup_date         DATE,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP
);


-- ------------------------------------------------------------
-- 3. transactions
-- ------------------------------------------------------------
CREATE TABLE transactions (
    id               UUID          PRIMARY KEY,
    user_id          UUID          NOT NULL,
    name             VARCHAR(120)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    type             VARCHAR(10)   NOT NULL,
    amount           NUMERIC(14,2) NOT NULL,
    transaction_date DATE          NOT NULL,
    note             VARCHAR(255),
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP
);


-- ------------------------------------------------------------
-- 4. goals
-- ------------------------------------------------------------
CREATE TABLE goals (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL,
    title         VARCHAR(120)  NOT NULL,
    category      VARCHAR(50),
    target_amount NUMERIC(14,2) NOT NULL,
    saved_amount  NUMERIC(14,2) NOT NULL DEFAULT 0,
    deadline      DATE,
    status        VARCHAR(20)   NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP
);


-- ------------------------------------------------------------
-- 5. savings_entries
-- ------------------------------------------------------------
CREATE TABLE savings_entries (
    id         UUID          PRIMARY KEY,
    user_id    UUID          NOT NULL,
    title      VARCHAR(120)  NOT NULL,
    category   VARCHAR(50),
    amount     NUMERIC(14,2) NOT NULL,
    entry_date DATE          NOT NULL,
    note       VARCHAR(255),
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);


-- ------------------------------------------------------------
-- 6. budgets
-- ------------------------------------------------------------
CREATE TABLE budgets (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL,
    category      VARCHAR(50)   NOT NULL,
    monthly_limit NUMERIC(14,2) NOT NULL,
    note          VARCHAR(255),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMP
);


-- ------------------------------------------------------------
-- 7. affordability_checks
-- ------------------------------------------------------------
CREATE TABLE affordability_checks (
    id               UUID          PRIMARY KEY,
    user_id          UUID          NOT NULL,
    item_name        VARCHAR(120)  NOT NULL,
    amount           NUMERIC(14,2) NOT NULL,
    available_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    verdict          VARCHAR(60),
    level            VARCHAR(20),
    priority         VARCHAR(20),
    check_date       DATE          NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP
);


-- ------------------------------------------------------------
-- 8. notifications
-- ------------------------------------------------------------
CREATE TABLE notifications (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    title      VARCHAR(120) NOT NULL,
    message    TEXT         NOT NULL,
    type       VARCHAR(20)  NOT NULL DEFAULT 'info',
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);


-- ------------------------------------------------------------
-- 9. reports
-- ------------------------------------------------------------
CREATE TABLE reports (
    id                UUID          PRIMARY KEY,
    user_id           UUID          NOT NULL,
    report_type       VARCHAR(40)   NOT NULL,
    from_date         DATE          NOT NULL,
    to_date           DATE          NOT NULL,
    total_income      NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_expense     NUMERIC(14,2) NOT NULL DEFAULT 0,
    net_amount        NUMERIC(14,2) NOT NULL DEFAULT 0,
    transaction_count INTEGER       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMP
);


-- ------------------------------------------------------------
-- 10. feedback
-- ------------------------------------------------------------
CREATE TABLE feedback (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    subject    VARCHAR(150) NOT NULL,
    category   VARCHAR(50),
    message    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'open',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);


-- ------------------------------------------------------------
-- 11. feedback_history (audit trail of feedback changes)
-- ------------------------------------------------------------
CREATE TABLE feedback_history (
    id          UUID         PRIMARY KEY,
    feedback_id UUID         NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    note        VARCHAR(255),
    changed_by  UUID,
    changed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP
);


-- ------------------------------------------------------------
-- 12. news (written by an admin / super admin user)
-- ------------------------------------------------------------
CREATE TABLE news (
    id           UUID         PRIMARY KEY,
    author_id    UUID         NOT NULL,
    title        VARCHAR(150) NOT NULL,
    body         TEXT         NOT NULL,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'normal',
    status       VARCHAR(20)  NOT NULL DEFAULT 'draft',
    published_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP
);
