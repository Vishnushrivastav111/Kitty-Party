-- ============================================================================
-- MicroVault - 08_multi_scenario_demo_dump.sql
-- Comprehensive Demo & Scenario Seed Script (4 Users + Admin + SuperAdmin)
--
-- Prerequisites:
--   Ensure 02_create_tables.sql, 03_constraints.sql, 04_indexes.sql,
--   07_planning_enhancements.sql, and 07_approval_requests.sql have been run.
--   (This script also runs the additive ALTER TABLE IF NOT EXISTS checks at the top
--   so it works cleanly even if 07_* was not executed yet.)
--
-- Credentials (PBKDF2-SHA256 verified against PasswordHasher):
--   Super Admin : supermicrovault@microvault.com    / SuperAdmin@123
--   Admin       : admin@microvault.com              / Admin@1234
--   User 1 (4Y) : vikram.deshmukh@microvault.com    / User@1234
--   User 2 (1Y) : ananya.iyer@microvault.com        / User@1234
--   User 3 (2Y) : kabir.verma@microvault.com        / User@1234
--   User 4 (New): meera.nair@microvault.com         / User@1234
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 0. Ensure latest additive columns/tables exist before inserting
-- ----------------------------------------------------------------------------
ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS additional_income NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS other_commitments NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS emergency_reserve NUMERIC(14,2) NOT NULL DEFAULT 0;

ALTER TABLE goals ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE goals ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'medium';
ALTER TABLE goals ADD COLUMN IF NOT EXISTS planned_contribution NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE goals ADD COLUMN IF NOT EXISTS opening_saved NUMERIC(14,2) NOT NULL DEFAULT 0;

ALTER TABLE savings_entries ADD COLUMN IF NOT EXISTS goal_id UUID;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_key VARCHAR(200);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dismissed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS budget_id UUID;

ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS emi_months INTEGER;
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS monthly_emi NUMERIC(14,2);
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS total_repayment NUMERIC(14,2);

CREATE TABLE IF NOT EXISTS approval_requests (
    id           UUID PRIMARY KEY,
    requester_id UUID NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    resource_id  UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    reason       VARCHAR(500),
    reviewer_id  UUID,
    reviewed_at  TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP
);

-- Clean up previous runs of this specific demo dataset (by deterministic UUIDs/emails)
DELETE FROM approval_requests WHERE requester_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM feedback_history WHERE feedback_id IN (
    SELECT id FROM feedback WHERE user_id IN (
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000002',
        '30000000-0000-4000-8000-000000000003',
        '40000000-0000-4000-8000-000000000004'
    )
);
DELETE FROM feedback WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM reports WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM notifications WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM affordability_checks WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM budgets WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM savings_entries WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM goals WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM transactions WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM finance_profiles WHERE user_id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);
DELETE FROM users WHERE id IN (
    '10000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000004'
);

-- ============================================================================
-- 1. USERS (Admins + 4 Multi-Scenario Users)
-- ============================================================================

-- Super Admin (Password: SuperAdmin@123)
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
SELECT 'a1111111-1111-4111-8111-111111111111',
       'Super Admin',
       'supermicrovault@microvault.com',
       '9999999999',
       'pbkdf2_sha256$120000$ROodtv5D9MECStZXbB1MMA==$t4rnlWFjGzyFwlf1/vh3i7zr2+SJzaTlHmspQQa0daE=',
       'superadmin',
       'active',
       TIMESTAMP '2022-01-01 09:00:00',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'supermicrovault@microvault.com');

-- Admin (Password: Admin@1234)
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
SELECT 'a2222222-2222-4222-8222-222222222222',
       'Neha Admin',
       'admin@microvault.com',
       '9001122334',
       'pbkdf2_sha256$120000$IPvmMLAnDeu1w6GV744RPw==$+7AjmZGbLhnfsYmkcDKfQQSft5omlSJLnbRgtN9os9s=',
       'admin',
       'active',
       TIMESTAMP '2022-06-01 10:00:00',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@microvault.com');

-- USER 1: 4-Year Veteran Power User (Joined Oct 2022) - Disciplined Investor, Multi-Year History, Completed & Active Goals
-- Password: User@1234
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
VALUES (
    '10000000-0000-4000-8000-000000000001',
    'Vikram Pratap Deshmukh',
    'vikram.deshmukh@microvault.com',
    '9811122233',
    'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
    'user',
    'active',
    TIMESTAMP '2022-10-10 10:15:00',
    CURRENT_TIMESTAMP
);

-- USER 2: 1-Year Active User (Joined Oct 2025) - Young Professional, Strictly Within Budget, Goal Focused
-- Password: User@1234
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
VALUES (
    '20000000-0000-4000-8000-000000000002',
    'Ananya Sundar Iyer',
    'ananya.iyer@microvault.com',
    '9822233344',
    'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
    'user',
    'active',
    TIMESTAMP '2025-10-12 14:20:00',
    CURRENT_TIMESTAMP
);

-- USER 3: 2-Year Over-Budget & High-Loan User (Joined May 2024) - Demonstrates Budget Overrun & Tight Affordability
-- Password: User@1234
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
VALUES (
    '30000000-0000-4000-8000-000000000003',
    'Kabir Rajendra Verma',
    'kabir.verma@microvault.com',
    '9833344455',
    'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
    'user',
    'active',
    TIMESTAMP '2024-05-18 11:30:00',
    CURRENT_TIMESTAMP
);

-- USER 4: 3-Month New Starter User (Joined Jul 2026) - Early Stage Saver Building First Emergency Cushion
-- Password: User@1234
INSERT INTO users (id, full_name, email, phone, password_hash, role, status, created_at, updated_at)
VALUES (
    '40000000-0000-4000-8000-000000000004',
    'Meera Krishnan Nair',
    'meera.nair@microvault.com',
    '9844455566',
    'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
    'user',
    'active',
    TIMESTAMP '2026-07-05 16:45:00',
    CURRENT_TIMESTAMP
);


-- ============================================================================
-- 2. FINANCE PROFILES
-- ============================================================================

-- User 1 (Vikram - 4 Years): High earner, Home loan EMI, ₹75,000 monthly budget, ₹3,00,000 emergency reserve
INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, additional_income, pay_cycle,
    monthly_expenses, other_commitments, expense_categories,
    has_loan, loan_type, loan_amount, monthly_emi, emi_start_date,
    current_savings, emergency_reserve, savings_type, investments, investment_types,
    monthly_budget, budget_style, setup_date, created_at, updated_at
) VALUES (
    '11000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    'Salary', 165000.00, 20000.00, 'Monthly',
    62000.00, 10000.00, 'Housing,Food,Travel,Bills,Health,Shopping',
    TRUE, 'Home Loan', 3200000.00, 28000.00, DATE '2022-11-05',
    920000.00, 300000.00, 'Mutual Funds & Bank', 650000.00, 'Index Funds,PPF,Gold Bonds',
    75000.00, '50/30/20', DATE '2022-10-15',
    TIMESTAMP '2022-10-15 10:30:00', CURRENT_TIMESTAMP
);

-- User 2 (Ananya - 1 Year): Salaried ₹85,000, no loan, ₹40,000 monthly budget, ₹1,20,000 emergency reserve
INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, additional_income, pay_cycle,
    monthly_expenses, other_commitments, expense_categories,
    has_loan, loan_type, loan_amount, monthly_emi, emi_start_date,
    current_savings, emergency_reserve, savings_type, investments, investment_types,
    monthly_budget, budget_style, setup_date, created_at, updated_at
) VALUES (
    '21000000-0000-4000-8000-000000000002',
    '20000000-0000-4000-8000-000000000002',
    'Salary', 85000.00, 5000.00, 'Monthly',
    31500.00, 3000.00, 'Food,Housing,Travel,Bills,Entertainment',
    FALSE, NULL, 0.00, 0.00, NULL,
    240000.00, 90000.00, 'High-Yield Savings & SIP', 120000.00, 'Equity SIP,FD',
    40000.00, 'Zero-Based', DATE '2025-10-14',
    TIMESTAMP '2025-10-14 09:00:00', CURRENT_TIMESTAMP
);

-- User 3 (Kabir - 2 Years): Freelancer/Consultant ₹95,000, Car loan, ₹45,000 monthly budget, currently OVER BUDGET in Oct 2026
INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, additional_income, pay_cycle,
    monthly_expenses, other_commitments, expense_categories,
    has_loan, loan_type, loan_amount, monthly_emi, emi_start_date,
    current_savings, emergency_reserve, savings_type, investments, investment_types,
    monthly_budget, budget_style, setup_date, created_at, updated_at
) VALUES (
    '31000000-0000-4000-8000-000000000003',
    '30000000-0000-4000-8000-000000000003',
    'Freelance / Business', 95000.00, 0.00, 'Monthly',
    58500.00, 8000.00, 'Housing,Shopping,Food,Travel,Entertainment,Bills',
    TRUE, 'Vehicle Loan', 650000.00, 14500.00, DATE '2024-07-01',
    145000.00, 60000.00, 'Bank', 45000.00, 'Stocks',
    45000.00, 'Envelope', DATE '2024-05-20',
    TIMESTAMP '2024-05-20 12:00:00', CURRENT_TIMESTAMP
);

-- User 4 (Meera - 3 Months): Entry-level engineer ₹42,000, Education loan EMI, ₹24,000 monthly budget
INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, additional_income, pay_cycle,
    monthly_expenses, other_commitments, expense_categories,
    has_loan, loan_type, loan_amount, monthly_emi, emi_start_date,
    current_savings, emergency_reserve, savings_type, investments, investment_types,
    monthly_budget, budget_style, setup_date, created_at, updated_at
) VALUES (
    '41000000-0000-4000-8000-000000000004',
    '40000000-0000-4000-8000-000000000004',
    'Salary', 42000.00, 0.00, 'Monthly',
    19800.00, 2000.00, 'Housing,Food,Travel,Bills',
    TRUE, 'Education Loan', 220000.00, 4500.00, DATE '2026-07-10',
    38000.00, 20000.00, 'Bank & RD', 10000.00, 'Recurring Deposit',
    24000.00, '50/30/20', DATE '2026-07-06',
    TIMESTAMP '2026-07-06 18:00:00', CURRENT_TIMESTAMP
);


-- ============================================================================
-- 3. GOALS (Multiple Goals per User, Completed + Active + Paused)
-- ============================================================================

-- Vikram's Goals (4-Year User)
INSERT INTO goals (id, user_id, title, category, description, target_amount, saved_amount, opening_saved, deadline, priority, planned_contribution, status, created_at)
VALUES
('12000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '6-Month Emergency Reserve', 'Emergency', 'Liquid safety buffer in sweep-in FD', 300000.00, 300000.00, 100000.00, DATE '2026-12-31', 'high', 15000.00, 'completed', TIMESTAMP '2022-11-01 10:00:00'),
('12000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Home Loan Part-Prepayment', 'Debt', 'Reduce principal by 5 Lakhs before 2027', 500000.00, 340000.00, 120000.00, DATE '2027-06-30', 'high', 20000.00, 'active', TIMESTAMP '2024-01-10 10:00:00'),
('12000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'Japan Family Vacation', 'Travel', '10-day Tokyo & Kyoto family trip', 250000.00, 155000.00, 40000.00, DATE '2027-04-15', 'medium', 12000.00, 'active', TIMESTAMP '2025-06-01 10:00:00'),
('12000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'Child Higher Education Corpus', 'Education', 'Long-term equity index corpus', 1500000.00, 425000.00, 150000.00, DATE '2032-06-01', 'high', 18000.00, 'active', TIMESTAMP '2023-04-01 10:00:00');

-- Ananya's Goals (1-Year User)
INSERT INTO goals (id, user_id, title, category, description, target_amount, saved_amount, opening_saved, deadline, priority, planned_contribution, status, created_at)
VALUES
('22000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Core Emergency Fund', 'Emergency', '3 months of essential living costs', 120000.00, 96000.00, 30000.00, DATE '2027-01-31', 'high', 8000.00, 'active', TIMESTAMP '2025-10-15 11:00:00'),
('22000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'MacBook Pro M4 Upgrade', 'Gadgets', 'Workstation upgrade without EMI', 160000.00, 88000.00, 20000.00, DATE '2027-05-30', 'medium', 10000.00, 'active', TIMESTAMP '2026-01-10 11:00:00'),
('22000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', 'AWS & AI Certification', 'Education', 'Professional upskilling budget', 35000.00, 35000.00, 10000.00, DATE '2026-11-30', 'high', 5000.00, 'completed', TIMESTAMP '2025-11-01 11:00:00');

-- Kabir's Goals (2-Year Over-Budget User)
INSERT INTO goals (id, user_id, title, category, description, target_amount, saved_amount, opening_saved, deadline, priority, planned_contribution, status, created_at)
VALUES
('32000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Car Loan Foreclosure', 'Debt', 'Close vehicle loan early', 250000.00, 55000.00, 25000.00, DATE '2027-08-31', 'high', 10000.00, 'active', TIMESTAMP '2024-08-01 12:00:00'),
('32000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000003', 'Europe Photography Tour', 'Travel', 'Paused due to recent budget overruns', 180000.00, 30000.00, 30000.00, DATE '2027-10-15', 'low', 5000.00, 'paused', TIMESTAMP '2025-03-10 12:00:00');

-- Meera's Goals (3-Month Starter User)
INSERT INTO goals (id, user_id, title, category, description, target_amount, saved_amount, opening_saved, deadline, priority, planned_contribution, status, created_at)
VALUES
('42000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'Starter Emergency Cushion', 'Emergency', 'First 50k safety net', 50000.00, 23000.00, 8000.00, DATE '2027-03-31', 'high', 5000.00, 'active', TIMESTAMP '2026-07-07 10:00:00'),
('42000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000004', 'Two-Wheeler Down Payment', 'Vehicle', 'Scooter purchase for office commute', 40000.00, 12000.00, 0.00, DATE '2027-06-30', 'medium', 4000.00, 'active', TIMESTAMP '2026-08-01 10:00:00');


-- ============================================================================
-- 4. SAVINGS ENTRIES (Total Savings History Across Years)
-- ============================================================================

-- Vikram (Total Savings = ₹9,20,000 across 2022–2026)
INSERT INTO savings_entries (id, user_id, title, category, goal_id, amount, entry_date, note)
VALUES
('13000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '2022 Initial Vault & FD Transfer', 'Opening', '12000000-0000-4000-8000-000000000001', 180000.00, DATE '2022-11-10', 'Initial emergency corpus setup'),
('13000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '2023 Annual Bonus & SIP Accumulation', 'Deposit', '12000000-0000-4000-8000-000000000004', 220000.00, DATE '2023-11-15', 'Year 2 disciplined savings'),
('13000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '2024 Prepayment Sinking Fund', 'Deposit', '12000000-0000-4000-8000-000000000002', 210000.00, DATE '2024-11-12', 'Dedicated loan prepayment reserve'),
('13000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', '2025 Portfolio & Travel Deposit', 'Deposit', '12000000-0000-4000-8000-000000000003', 195000.00, DATE '2025-11-18', 'Travel + equity additions'),
('13000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 'Aug 2026 Monthly SIP & Reserve', 'Deposit', '12000000-0000-4000-8000-000000000002', 38000.00, DATE '2026-08-10', 'August contribution'),
('13000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', 'Sep 2026 Monthly SIP & Reserve', 'Deposit', '12000000-0000-4000-8000-000000000004', 40000.00, DATE '2026-09-10', 'September contribution'),
('13000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', 'Oct 2026 Monthly SIP & Travel Top-up', 'Deposit', '12000000-0000-4000-8000-000000000003', 37000.00, DATE '2026-10-10', 'Current month savings entry');

-- Ananya (Total Savings = ₹2,40,000 across 1 Year)
INSERT INTO savings_entries (id, user_id, title, category, goal_id, amount, entry_date, note)
VALUES
('23000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Opening Bank Savings', 'Opening', '22000000-0000-4000-8000-000000000001', 60000.00, DATE '2025-10-15', 'Starting balance when joining MicroVault'),
('23000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'Q1 2026 High-Yield Deposit', 'Deposit', '22000000-0000-4000-8000-000000000001', 55000.00, DATE '2026-03-15', 'Quarterly savings milestone'),
('23000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', 'Q2 2026 Laptop Goal Deposit', 'Deposit', '22000000-0000-4000-8000-000000000002', 65000.00, DATE '2026-06-15', 'Saved from bonus'),
('23000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000002', 'Aug 2026 SIP & Goal Transfer', 'Deposit', '22000000-0000-4000-8000-000000000002', 20000.00, DATE '2026-08-08', 'August savings'),
('23000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', 'Sep 2026 SIP & Goal Transfer', 'Deposit', '22000000-0000-4000-8000-000000000001', 20000.00, DATE '2026-09-08', 'September savings'),
('23000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000002', 'Oct 2026 Monthly Deposit', 'Deposit', '22000000-0000-4000-8000-000000000002', 20000.00, DATE '2026-10-08', 'October monthly savings');

-- Kabir (Total Savings = ₹1,45,000 across 2 Years)
INSERT INTO savings_entries (id, user_id, title, category, goal_id, amount, entry_date, note)
VALUES
('33000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', '2024 Initial Freelance Reserve', 'Opening', NULL, 80000.00, DATE '2024-06-01', 'Emergency & tax buffer'),
('33000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000003', '2025 Loan Prepayment Bucket', 'Deposit', '32000000-0000-4000-8000-000000000001', 45000.00, DATE '2025-09-10', 'Client project surplus'),
('33000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000003', 'Aug 2026 Savings Deposit', 'Deposit', '32000000-0000-4000-8000-000000000001', 12000.00, DATE '2026-08-14', 'August deposit'),
('33000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000003', 'Oct 2026 Small Transfer', 'Deposit', NULL, 8000.00, DATE '2026-10-05', 'Kept savings untouched despite October budget overrun');

-- Meera (Total Savings = ₹38,000 across 3 Months)
INSERT INTO savings_entries (id, user_id, title, category, goal_id, amount, entry_date, note)
VALUES
('43000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'Joining Bonus Savings', 'Opening', '42000000-0000-4000-8000-000000000001', 18000.00, DATE '2026-07-10', 'First paycheck reserve'),
('43000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000004', 'Aug 2026 RD & Emergency', 'Deposit', '42000000-0000-4000-8000-000000000001', 7000.00, DATE '2026-08-07', 'Month 2 savings'),
('43000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000004', 'Sep 2026 Two-Wheeler Fund', 'Deposit', '42000000-0000-4000-8000-000000000002', 6500.00, DATE '2026-09-07', 'Month 3 savings'),
('43000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000004', 'Oct 2026 Monthly RD', 'Deposit', '42000000-0000-4000-8000-000000000002', 6500.00, DATE '2026-10-07', 'Current month savings');


-- ============================================================================
-- 5. BUDGETS (Category Monthly Limits)
-- ============================================================================

-- Vikram (Total Category Limits = ₹75,000 matching monthly_budget)
INSERT INTO budgets (id, user_id, category, monthly_limit, note)
VALUES
('14000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Housing', 25000.00, 'Society maintenance, property upkeep & utilities'),
('14000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Food', 16000.00, 'Family groceries and weekend dining'),
('14000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'Travel', 10000.00, 'Fuel, FASTag, and city commute'),
('14000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'Bills', 9000.00, 'Electricity, fiber broadband, mobile plans'),
('14000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 'Shopping', 15000.00, 'Household & apparel');

-- Ananya (Total Category Limits = ₹40,000 matching monthly_budget)
INSERT INTO budgets (id, user_id, category, monthly_limit, note)
VALUES
('24000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Housing', 16000.00, 'Shared flat rent'),
('24000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'Food', 10000.00, 'Groceries, tiffin & cafes'),
('24000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', 'Travel', 5000.00, 'Metro pass & cabs'),
('24000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000002', 'Bills', 4000.00, 'Wi-Fi, electricity share & subscriptions'),
('24000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', 'Entertainment', 5000.00, 'Movies, books & fitness');

-- Kabir (Total Category Limits = ₹45,000; October actual spending is ₹58,500 -> ₹13,500 OVERRUN)
INSERT INTO budgets (id, user_id, category, monthly_limit, note)
VALUES
('34000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Housing', 20000.00, 'Studio apartment rent'),
('34000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000003', 'Food', 9000.00, 'Dining & food delivery'),
('34000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000003', 'Shopping', 8000.00, 'Camera gear & lifestyle'),
('34000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000003', 'Travel', 8000.00, 'Fuel & client travel');

-- Meera (Total Category Limits = ₹24,000)
INSERT INTO budgets (id, user_id, category, monthly_limit, note)
VALUES
('44000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'Housing', 10000.00, 'PG accommodation'),
('44000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000004', 'Food', 6500.00, 'Meals & snacks'),
('44000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000004', 'Travel', 4000.00, 'Bus & metro'),
('44000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000004', 'Bills', 3500.00, 'Mobile & utilities');


-- ============================================================================
-- 6. TRANSACTIONS (Multi-Year & Current-Month Oct 2026 Scenarios)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- USER 1: Vikram (4-Year History: 2022, 2023, 2024, 2025 + Aug/Sep/Oct 2026)
-- Current Month (Oct 2026): Budget ₹75,000 | Oct Expenses ₹54,200 | Remaining ₹20,800
-- ----------------------------------------------------------------------------
INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
VALUES
-- Historical 2022–2025 milestones
('15000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Oct 2022 Salary Credit', 'Salary', 'income', 125000.00, DATE '2022-10-31', 'First tracked salary in MicroVault'),
('15000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Nov 2022 Home Setup & Rent', 'Housing', 'expense', 38000.00, DATE '2022-11-05', 'Year 1 housing expense'),
('15000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'Oct 2023 Appraisal Salary', 'Salary', 'income', 140000.00, DATE '2023-10-31', 'Year 2 salary credit'),
('15000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'Nov 2023 Diwali Family Shopping', 'Shopping', 'expense', 29500.00, DATE '2023-11-10', 'Festival shopping'),
('15000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 'Oct 2024 Lead Architect Salary', 'Salary', 'income', 155000.00, DATE '2024-10-31', 'Year 3 salary credit'),
('15000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', 'Dec 2024 Annual Health Checkup', 'Health', 'expense', 18000.00, DATE '2024-12-14', 'Preventive family health package'),
('15000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', 'Oct 2025 Principal Architect Salary', 'Salary', 'income', 165000.00, DATE '2025-10-31', 'Year 4 salary credit'),
-- Recent 2026 months (Aug & Sep)
('15000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000001', 'August 2026 Salary', 'Salary', 'income', 165000.00, DATE '2026-08-01', 'Monthly salary'),
('15000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000001', 'August Housing & Maintenance', 'Housing', 'expense', 24000.00, DATE '2026-08-05', 'Society & upkeep'),
('15000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000001', 'August Family Groceries', 'Food', 'expense', 14500.00, DATE '2026-08-12', 'Monthly provisions'),
('15000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', 'September 2026 Salary', 'Salary', 'income', 165000.00, DATE '2026-09-01', 'Monthly salary'),
('15000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000001', 'September Housing & Maintenance', 'Housing', 'expense', 24000.00, DATE '2026-09-05', 'Society & upkeep'),
('15000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000001', 'September Groceries & Dining', 'Food', 'expense', 15200.00, DATE '2026-09-14', 'Groceries + family dinner'),
-- Current Month: October 2026 (Total Expenses = ₹54,200 against ₹75,000 budget)
('15000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000001', 'October 2026 Salary', 'Salary', 'income', 165000.00, DATE '2026-10-01', 'October salary credit'),
('15000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000001', 'Quarterly Dividend Payout', 'Investment', 'income', 20000.00, DATE '2026-10-03', 'Index & equity dividend'),
('15000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000001', 'Society Maintenance & Property Tax', 'Housing', 'expense', 22500.00, DATE '2026-10-04', 'October housing'),
('15000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000001', 'Supermarket & Organic Groceries', 'Food', 'expense', 12400.00, DATE '2026-10-07', 'Monthly grocery stock'),
('15000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000001', 'Highway Fuel & FASTag Recharge', 'Travel', 'expense', 6800.00, DATE '2026-10-09', 'Commute & weekend drive'),
('15000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000001', 'Electricity, Fiber & Postpaid Bills', 'Bills', 'expense', 7500.00, DATE '2026-10-11', 'Monthly utilities'),
('15000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000001', 'Festive Apparel Shopping', 'Shopping', 'expense', 5000.00, DATE '2026-10-13', 'Festive gifts');

-- ----------------------------------------------------------------------------
-- USER 2: Ananya (1-Year History: Oct 2025 – Oct 2026)
-- Current Month (Oct 2026): Budget ₹40,000 | Oct Expenses ₹28,400 | Remaining ₹11,600
-- ----------------------------------------------------------------------------
INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
VALUES
('25000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Nov 2025 Salary', 'Salary', 'income', 80000.00, DATE '2025-11-01', 'Initial salary'),
('25000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'Nov 2025 Flat Rent', 'Housing', 'expense', 16000.00, DATE '2025-11-04', 'Monthly rent'),
('25000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', 'Mar 2026 Salary', 'Salary', 'income', 85000.00, DATE '2026-03-01', 'Post-increment salary'),
('25000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000002', 'Aug 2026 Salary', 'Salary', 'income', 85000.00, DATE '2026-08-01', 'August salary'),
('25000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', 'Aug 2026 Rent & Groceries', 'Housing', 'expense', 24200.00, DATE '2026-08-05', 'August living expenses'),
('25000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000002', 'Sep 2026 Salary', 'Salary', 'income', 85000.00, DATE '2026-09-01', 'September salary'),
('25000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000002', 'Sep 2026 Rent & Utilities', 'Housing', 'expense', 26800.00, DATE '2026-09-05', 'September living expenses'),
-- Current Month: October 2026 (Total Expenses = ₹28,400 against ₹40,000 budget)
('25000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000002', 'October 2026 Salary', 'Salary', 'income', 85000.00, DATE '2026-10-01', 'October salary'),
('25000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000002', 'UI Design Workshop Honorarium', 'Freelance', 'income', 5000.00, DATE '2026-10-06', 'Weekend mentoring session'),
('25000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000002', 'October Flat Rent', 'Housing', 'expense', 16000.00, DATE '2026-10-03', 'Monthly rent'),
('25000000-0000-4000-8000-000000000011', '20000000-0000-4000-8000-000000000002', 'Groceries & Meal Prep', 'Food', 'expense', 6200.00, DATE '2026-10-08', 'Fresh produce & groceries'),
('25000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000002', 'Metro Card & Cab Rides', 'Travel', 'expense', 2400.00, DATE '2026-10-10', 'Office commute'),
('25000000-0000-4000-8000-000000000013', '20000000-0000-4000-8000-000000000002', 'Broadband & Electricity Share', 'Bills', 'expense', 2300.00, DATE '2026-10-11', 'Utilities'),
('25000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000002', 'Badminton Club & Book Fair', 'Entertainment', 'expense', 1500.00, DATE '2026-10-13', 'Weekend leisure');

-- ----------------------------------------------------------------------------
-- USER 3: Kabir (2-Year History + Budget Overrun Scenario in Oct 2026)
-- Current Month (Oct 2026): Budget ₹45,000 | Oct Expenses ₹58,500 | Overrun = ₹13,500!
-- ----------------------------------------------------------------------------
INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
VALUES
('35000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Jun 2024 Retainer Invoice', 'Business', 'income', 90000.00, DATE '2024-06-05', 'Year 1 consulting'),
('35000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000003', 'Sep 2025 Brand Campaign Payout', 'Business', 'income', 95000.00, DATE '2025-09-04', 'Year 2 consulting'),
('35000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000003', 'Sep 2026 Retainer Invoice', 'Business', 'income', 95000.00, DATE '2026-09-02', 'September retainer'),
('35000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000003', 'Sep 2026 Studio Rent & Gear', 'Housing', 'expense', 39000.00, DATE '2026-09-08', 'September expenses'),
-- Current Month: October 2026 (Total Expenses = ₹58,500 vs ₹45,000 budget -> ₹13,500 Overrun)
('35000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000003', 'October 2026 Client Retainer', 'Business', 'income', 95000.00, DATE '2026-10-02', 'Monthly retainer'),
('35000000-0000-4000-8000-000000000006', '30000000-0000-4000-8000-000000000003', 'Studio & Apartment Rent', 'Housing', 'expense', 20000.00, DATE '2026-10-03', 'Fixed rent'),
('35000000-0000-4000-8000-000000000007', '30000000-0000-4000-8000-000000000003', 'Dining Out & Client Lunches', 'Food', 'expense', 11500.00, DATE '2026-10-06', 'Exceeded ₹9,000 food budget'),
('35000000-0000-4000-8000-000000000008', '30000000-0000-4000-8000-000000000003', 'Mirrorless Lens Repair & Gimbal', 'Shopping', 'expense', 17500.00, DATE '2026-10-09', 'Urgent equipment purchase caused budget overrun'),
('35000000-0000-4000-8000-000000000009', '30000000-0000-4000-8000-000000000003', 'Outstation Shoot Fuel & Flights', 'Travel', 'expense', 9500.00, DATE '2026-10-12', 'Exceeded travel budget');

-- ----------------------------------------------------------------------------
-- USER 4: Meera (3-Month Starter History: Jul 2026 – Oct 2026)
-- Current Month (Oct 2026): Budget ₹24,000 | Oct Expenses ₹17,900 | Remaining ₹6,100
-- ----------------------------------------------------------------------------
INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
VALUES
('45000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'August 2026 Salary', 'Salary', 'income', 42000.00, DATE '2026-08-01', 'Month 2 salary'),
('45000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000004', 'August PG Rent & Mess', 'Housing', 'expense', 15500.00, DATE '2026-08-04', 'August living cost'),
('45000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000004', 'September 2026 Salary', 'Salary', 'income', 42000.00, DATE '2026-09-01', 'Month 3 salary'),
('45000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000004', 'September PG Rent & Commute', 'Housing', 'expense', 16200.00, DATE '2026-09-04', 'September living cost'),
-- Current Month: October 2026 (Total Expenses = ₹17,900 against ₹24,000 budget)
('45000000-0000-4000-8000-000000000005', '40000000-0000-4000-8000-000000000004', 'October 2026 Salary', 'Salary', 'income', 42000.00, DATE '2026-10-01', 'October salary'),
('45000000-0000-4000-8000-000000000006', '40000000-0000-4000-8000-000000000004', 'PG Accommodation Rent', 'Housing', 'expense', 10000.00, DATE '2026-10-02', 'October PG rent'),
('45000000-0000-4000-8000-000000000007', '40000000-0000-4000-8000-000000000004', 'Canteen & Daily Meals', 'Food', 'expense', 4300.00, DATE '2026-10-07', 'Food expenses'),
('45000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000004', 'Metro & Bus Pass', 'Travel', 'expense', 1800.00, DATE '2026-10-09', 'Monthly transit'),
('45000000-0000-4000-8000-000000000009', '40000000-0000-4000-8000-000000000004', 'Prepaid Mobile & Study Materials', 'Bills', 'expense', 1800.00, DATE '2026-10-12', 'Phone & books');


-- ============================================================================
-- 7. AFFORDABILITY CHECKS (Success, Warning, and Danger Verdicts)
-- ============================================================================
INSERT INTO affordability_checks (
    id, user_id, item_name, amount, available_amount, verdict, level, priority,
    payment_method, emi_months, monthly_emi, total_repayment, check_date
) VALUES
('16000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Solar Rooftop System', 140000.00, 299000.00, 'Easily Affordable', 'success', 'high', 'cash', NULL, NULL, NULL, DATE '2026-10-12'),
('26000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Ergonomic Standing Desk & Monitor', 42000.00, 125500.00, 'Affordable', 'success', 'medium', 'cash', NULL, NULL, NULL, DATE '2026-10-11'),
('36000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Cinema Camera Body Upgrade', 185000.00, 80000.00, 'Not Recommended Right Now', 'danger', 'low', 'emi', 12, 16800.00, 201600.00, DATE '2026-10-13'),
('46000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'Noise-Cancelling Headphones', 18000.00, 33600.00, 'Affordable with Caution', 'warning', 'low', 'cash', NULL, NULL, NULL, DATE '2026-10-12');


-- ============================================================================
-- 8. REPORTS (Generated Financial Summaries)
-- ============================================================================
INSERT INTO reports (id, user_id, report_type, from_date, to_date, total_income, total_expense, net_amount, transaction_count, created_at)
VALUES
('17000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Q3 2026 Executive Review', DATE '2026-08-01', DATE '2026-09-30', 330000.00, 77700.00, 252300.00, 6, TIMESTAMP '2026-10-01 18:00:00'),
('17000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'October 2026 Mid-Month Check', DATE '2026-10-01', DATE '2026-10-15', 185000.00, 54200.00, 130800.00, 7, TIMESTAMP '2026-10-14 09:30:00'),
('27000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', '1-Year Anniversary Summary', DATE '2025-10-15', DATE '2026-10-15', 425000.00, 95400.00, 329600.00, 14, TIMESTAMP '2026-10-14 11:00:00'),
('37000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'October Overrun Audit', DATE '2026-10-01', DATE '2026-10-15', 95000.00, 58500.00, 36500.00, 5, TIMESTAMP '2026-10-14 15:00:00'),
('47000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'First Quarter Tracker', DATE '2026-08-01', DATE '2026-10-15', 126000.00, 49600.00, 76400.00, 9, TIMESTAMP '2026-10-14 16:00:00');


-- ============================================================================
-- 9. NOTIFICATIONS (Budget Alerts, Goal Milestones, System Messages)
-- ============================================================================
INSERT INTO notifications (id, user_id, title, message, type, is_read, created_at)
VALUES
('18000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '4-Year MicroVault Milestone', 'You have completed 4 years of disciplined tracking with ₹9,20,000 in total savings.', 'success', FALSE, TIMESTAMP '2026-10-10 10:00:00'),
('18000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Emergency Reserve Completed', 'Your 6-Month Emergency Reserve goal of ₹3,00,000 is 100% funded.', 'info', TRUE, TIMESTAMP '2026-09-15 12:00:00'),
('28000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Core Emergency Fund at 80%', 'You have saved ₹96,000 of your ₹1,20,000 emergency goal.', 'info', FALSE, TIMESTAMP '2026-10-09 09:00:00'),
('38000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Monthly Budget Exceeded', 'October spending (₹58,500) exceeded your ₹45,000 monthly budget by ₹13,500.', 'danger', FALSE, TIMESTAMP '2026-10-12 19:30:00'),
('38000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000003', 'Shopping Category Overrun', 'Shopping expenses (₹17,500) exceeded the ₹8,000 category limit by ₹9,500.', 'warning', FALSE, TIMESTAMP '2026-10-09 20:00:00'),
('48000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000004', 'Great Start on Your First Goal', 'Your Starter Emergency Cushion has reached 46% completion.', 'success', FALSE, TIMESTAMP '2026-10-08 10:00:00');


-- ============================================================================
-- 10. FEEDBACK & FEEDBACK HISTORY (Open, In-Review, Resolved)
-- ============================================================================
INSERT INTO feedback (id, user_id, subject, category, message, status, created_at)
VALUES
('19000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Multi-year tax export worked smoothly', 'General', 'Exporting my 4-year summary helped with my annual CA review.', 'resolved', TIMESTAMP '2026-09-20 11:00:00'),
('29000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'Goal progress vs total savings clarity', 'Feature', 'Loving the separate goal contribution column so my emergency fund is not mixed with gadget savings.', 'in-review', TIMESTAMP '2026-10-10 15:00:00'),
('39000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 'Request to delete duplicate lens expense', 'Support', 'I submitted a transaction deletion request for review in the approval center.', 'open', TIMESTAMP '2026-10-13 17:00:00');

INSERT INTO feedback_history (id, feedback_id, action, note, changed_by, changed_at)
VALUES
('19100000-0000-4000-8000-000000000001', '19000000-0000-4000-8000-000000000001', 'resolved', 'Acknowledged by Admin Neha', 'a2222222-2222-4222-8222-222222222222', TIMESTAMP '2026-09-21 10:00:00'),
('29100000-0000-4000-8000-000000000001', '29000000-0000-4000-8000-000000000001', 'in-review', 'Shared with product team', 'a2222222-2222-4222-8222-222222222222', TIMESTAMP '2026-10-11 11:00:00'),
('39100000-0000-4000-8000-000000000001', '39000000-0000-4000-8000-000000000001', 'created', 'Submitted by member', '30000000-0000-4000-8000-000000000003', TIMESTAMP '2026-10-13 17:00:00');


-- ============================================================================
-- 11. APPROVAL REQUESTS (Pending & Reviewed Admin Approval Center Records)
-- ============================================================================
INSERT INTO approval_requests (
    id, requester_id, request_type, resource_id, status, reason, reviewer_id, reviewed_at, created_at
) VALUES
(
    '39900000-0000-4000-8000-000000000001',
    '30000000-0000-4000-8000-000000000003',
    'TRANSACTION_DELETE',
    '35000000-0000-4000-8000-000000000008',
    'pending',
    'Vendor offered partial warranty coverage; requesting deletion to re-enter revised bill.',
    NULL,
    NULL,
    TIMESTAMP '2026-10-13 16:50:00'
),
(
    '29900000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000002',
    'TRANSACTION_DELETE',
    '25000000-0000-4000-8000-000000000014',
    'rejected',
    'Accidental click; expense is valid and confirmed.',
    'a2222222-2222-4222-8222-222222222222',
    TIMESTAMP '2026-10-14 10:15:00',
    TIMESTAMP '2026-10-13 12:00:00'
);


-- ============================================================================
-- 12. NEWS (Published Financial Updates)
-- ============================================================================
INSERT INTO news (id, author_id, title, body, priority, status, published_at)
SELECT '99000000-0000-4000-8000-000000000001',
       'a2222222-2222-4222-8222-222222222222',
       'Keep Your Emergency Reserve Separate from Goal Savings',
       'Always maintain 3 to 6 months of essential expenses in your emergency reserve before committing surplus funds to discretionary purchases.',
       'high',
       'published',
       TIMESTAMP '2026-10-10 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM news WHERE id = '99000000-0000-4000-8000-000000000001');

COMMIT;
