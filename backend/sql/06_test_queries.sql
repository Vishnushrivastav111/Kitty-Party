-- ============================================================
-- MicroVault - 06_test_queries.sql
-- Manual checks for every CRUD path. These statements do not
-- hard-delete business rows. Soft-delete examples use UPDATE.
-- ============================================================

-- ------------------------------------------------------------
-- USER
-- ------------------------------------------------------------
-- INSERT (example only; prefer 05_seed_data.sql for real rows)
-- INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
-- VALUES (gen_random_uuid(), 'Test User', 'test.user@example.com', '9876500000',
--         'replace-with-hash', 'user', 'active');

SELECT * FROM users WHERE id = 'a3333333-3333-4333-8333-333333333333' AND is_deleted = FALSE;
SELECT * FROM users WHERE is_deleted = FALSE ORDER BY created_at DESC;
SELECT * FROM users WHERE LOWER(email) = LOWER('aarav.sharma@example.com') AND is_deleted = FALSE;
SELECT * FROM users WHERE status = 'active' AND is_deleted = FALSE;
SELECT * FROM users WHERE role = 'admin' AND is_deleted = FALSE;

UPDATE users
SET full_name = 'Aarav Sharma',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

-- Soft delete example (do not run against seed users unless you intend to):
-- UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
-- WHERE id = ? AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- FINANCE PROFILE
-- ------------------------------------------------------------
SELECT * FROM finance_profiles
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM finance_profiles WHERE is_deleted = FALSE;

UPDATE finance_profiles
SET monthly_income = 85000.00,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- TRANSACTION
-- ------------------------------------------------------------
SELECT * FROM transactions
WHERE id = 'c1111111-1111-4111-8111-111111111111'
  AND is_deleted = FALSE;

SELECT * FROM transactions
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE
ORDER BY transaction_date DESC;

SELECT * FROM transactions
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND transaction_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-31'
  AND is_deleted = FALSE;

SELECT * FROM transactions
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND category = 'Food'
  AND is_deleted = FALSE;

UPDATE transactions
SET amount = 4200.00,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'c2222222-2222-4222-8222-222222222222'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- GOAL
-- ------------------------------------------------------------
SELECT * FROM goals
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM goals
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND status = 'active'
  AND is_deleted = FALSE;

UPDATE goals
SET saved_amount = 75000.00,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd1111111-1111-4111-8111-111111111111'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- SAVINGS ENTRY
-- ------------------------------------------------------------
SELECT * FROM savings_entries
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM savings_entries
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND entry_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-31'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- BUDGET
-- ------------------------------------------------------------
SELECT * FROM budgets
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM budgets
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND category = 'Food'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- AFFORDABILITY CHECK
-- ------------------------------------------------------------
SELECT * FROM affordability_checks
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- NOTIFICATION
-- ------------------------------------------------------------
SELECT * FROM notifications
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE
ORDER BY created_at DESC;

SELECT * FROM notifications
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_read = FALSE
  AND is_deleted = FALSE;

UPDATE notifications
SET is_read = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- REPORT
-- ------------------------------------------------------------
SELECT * FROM reports
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM reports
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND from_date >= DATE '2026-08-01'
  AND to_date <= DATE '2026-08-31'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- FEEDBACK AND HISTORY
-- ------------------------------------------------------------
SELECT * FROM feedback
WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
  AND is_deleted = FALSE;

SELECT * FROM feedback
WHERE status = 'open'
  AND is_deleted = FALSE;

SELECT * FROM feedback_history
WHERE feedback_id = '14111111-1111-4111-8111-111111111111'
  AND is_deleted = FALSE
ORDER BY changed_at;


-- ------------------------------------------------------------
-- NEWS
-- ------------------------------------------------------------
SELECT * FROM news
WHERE status = 'published'
  AND is_deleted = FALSE
ORDER BY published_at DESC;

SELECT * FROM news
WHERE author_id = 'a2222222-2222-4222-8222-222222222222'
  AND is_deleted = FALSE;


-- ------------------------------------------------------------
-- SOFT DELETE PATTERN (all business tables)
-- ------------------------------------------------------------
-- UPDATE <table>
-- SET is_deleted = TRUE,
--     deleted_at = CURRENT_TIMESTAMP,
--     updated_at = CURRENT_TIMESTAMP
-- WHERE id = ?
--   AND is_deleted = FALSE;

-- Normal reads always exclude deleted rows:
-- WHERE is_deleted = FALSE
