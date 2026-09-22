-- ============================================================
-- MicroVault - 05_seed_data.sql
-- Demo rows that match the existing frontend accounts.
-- Passwords are stored as PBKDF2 hashes, never as plain text.
--
--   Super admin : supermicrovault@microvault.com / SuperAdmin@123
--   Admin       : admin@microvault.com           / Admin@1234
--   Members     : aarav / priya / rohan          / User@1234
--
-- Run after 02, 03 and 04. Safe to re-run: existing emails are skipped.
-- ============================================================

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
SELECT 'a1111111-1111-4111-8111-111111111111',
       'Super Admin',
       'supermicrovault@microvault.com',
       '9999999999',
       'pbkdf2_sha256$120000$ROodtv5D9MECStZXbB1MMA==$t4rnlWFjGzyFwlf1/vh3i7zr2+SJzaTlHmspQQa0daE=',
       'superadmin',
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'supermicrovault@microvault.com'
);

INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
SELECT 'a2222222-2222-4222-8222-222222222222',
       'Neha Admin',
       'admin@microvault.com',
       '9001122334',
       'pbkdf2_sha256$120000$IPvmMLAnDeu1w6GV744RPw==$+7AjmZGbLhnfsYmkcDKfQQSft5omlSJLnbRgtN9os9s=',
       'admin',
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@microvault.com'
);

INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
SELECT 'a3333333-3333-4333-8333-333333333333',
       'Aarav Sharma',
       'aarav.sharma@example.com',
       '9876543210',
       'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
       'user',
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'aarav.sharma@example.com'
);

INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
SELECT 'a4444444-4444-4444-8444-444444444444',
       'Priya Patel',
       'priya.patel@example.com',
       '9123456780',
       'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
       'user',
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'priya.patel@example.com'
);

INSERT INTO users (id, full_name, email, phone, password_hash, role, status)
SELECT 'a5555555-5555-4555-8555-555555555555',
       'Rohan Mehta',
       'rohan.mehta@example.com',
       '9988776655',
       'pbkdf2_sha256$120000$wDYVwguxnzD/G4kp/k8KqQ==$nDB01dO4P46XOSoURDlW6/h9cFerlRWmbUhCCvjnAWM=',
       'user',
       'inactive'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'rohan.mehta@example.com'
);


-- ------------------------------------------------------------
-- finance_profiles
-- ------------------------------------------------------------
INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, pay_cycle, monthly_expenses,
    expense_categories, has_loan, current_savings, savings_type,
    investments, monthly_budget, budget_style, setup_date
)
SELECT 'b1111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Salary',
       85000.00,
       'Monthly',
       42000.00,
       'Food,Travel,Bills',
       FALSE,
       150000.00,
       'Bank',
       40000.00,
       40000.00,
       '50/30/20',
       DATE '2026-01-15'
WHERE NOT EXISTS (
    SELECT 1 FROM finance_profiles
    WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
      AND is_deleted = FALSE
);

INSERT INTO finance_profiles (
    id, user_id, income_source, monthly_income, pay_cycle, monthly_expenses,
    has_loan, current_savings, monthly_budget, setup_date
)
SELECT 'b2222222-2222-4222-8222-222222222222',
       'a4444444-4444-4444-8444-444444444444',
       'Business',
       62000.00,
       'Monthly',
       31000.00,
       FALSE,
       80000.00,
       28000.00,
       DATE '2026-02-01'
WHERE NOT EXISTS (
    SELECT 1 FROM finance_profiles
    WHERE user_id = 'a4444444-4444-4444-8444-444444444444'
      AND is_deleted = FALSE
);


-- ------------------------------------------------------------
-- transactions
-- ------------------------------------------------------------
INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
SELECT 'c1111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Monthly salary',
       'Salary',
       'income',
       85000.00,
       DATE '2026-08-01',
       'August salary'
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE id = 'c1111111-1111-4111-8111-111111111111'
);

INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
SELECT 'c2222222-2222-4222-8222-222222222222',
       'a3333333-3333-4333-8333-333333333333',
       'Grocery run',
       'Food',
       'expense',
       4200.00,
       DATE '2026-08-05',
       'Weekly groceries'
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE id = 'c2222222-2222-4222-8222-222222222222'
);

INSERT INTO transactions (id, user_id, name, category, type, amount, transaction_date, note)
SELECT 'c3333333-3333-4333-8333-333333333333',
       'a3333333-3333-4333-8333-333333333333',
       'Metro card',
       'Travel',
       'expense',
       1500.00,
       DATE '2026-08-08',
       NULL
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE id = 'c3333333-3333-4333-8333-333333333333'
);


-- ------------------------------------------------------------
-- goals
-- ------------------------------------------------------------
INSERT INTO goals (id, user_id, title, category, target_amount, saved_amount, deadline, status)
SELECT 'd1111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Emergency fund',
       'Savings',
       200000.00,
       75000.00,
       DATE '2027-03-31',
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM goals WHERE id = 'd1111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- savings_entries
-- ------------------------------------------------------------
INSERT INTO savings_entries (id, user_id, title, category, amount, entry_date, note)
SELECT 'e1111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'August SIP',
       'Investment',
       8000.00,
       DATE '2026-08-10',
       'Monthly SIP'
WHERE NOT EXISTS (
    SELECT 1 FROM savings_entries WHERE id = 'e1111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- budgets
-- ------------------------------------------------------------
INSERT INTO budgets (id, user_id, category, monthly_limit, note)
SELECT 'f1111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Food',
       12000.00,
       'Groceries and eating out'
WHERE NOT EXISTS (
    SELECT 1 FROM budgets
    WHERE user_id = 'a3333333-3333-4333-8333-333333333333'
      AND category = 'Food'
      AND is_deleted = FALSE
);


-- ------------------------------------------------------------
-- affordability_checks
-- ------------------------------------------------------------
INSERT INTO affordability_checks (
    id, user_id, item_name, amount, available_amount, verdict, level, priority, check_date
)
SELECT '11111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'New laptop',
       55000.00,
       88000.00,
       'Affordable with caution',
       'warning',
       'medium',
       DATE '2026-08-12'
WHERE NOT EXISTS (
    SELECT 1 FROM affordability_checks WHERE id = '11111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- notifications
-- ------------------------------------------------------------
INSERT INTO notifications (id, user_id, title, message, type, is_read)
SELECT '12111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Welcome to MicroVault',
       'Your financial setup is ready. Start adding transactions.',
       'info',
       FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM notifications WHERE id = '12111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- reports
-- ------------------------------------------------------------
INSERT INTO reports (
    id, user_id, report_type, from_date, to_date,
    total_income, total_expense, net_amount, transaction_count
)
SELECT '13111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Monthly',
       DATE '2026-08-01',
       DATE '2026-08-31',
       85000.00,
       5700.00,
       79300.00,
       3
WHERE NOT EXISTS (
    SELECT 1 FROM reports WHERE id = '13111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- feedback + history
-- ------------------------------------------------------------
INSERT INTO feedback (id, user_id, subject, category, message, status)
SELECT '14111111-1111-4111-8111-111111111111',
       'a3333333-3333-4333-8333-333333333333',
       'Budget chart request',
       'Feature',
       'Please add a weekly view on the budget page.',
       'open'
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE id = '14111111-1111-4111-8111-111111111111'
);

INSERT INTO feedback_history (id, feedback_id, action, note, changed_by, changed_at)
SELECT '15111111-1111-4111-8111-111111111111',
       '14111111-1111-4111-8111-111111111111',
       'created',
       'Member submitted feedback',
       'a3333333-3333-4333-8333-333333333333',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM feedback_history WHERE id = '15111111-1111-4111-8111-111111111111'
);


-- ------------------------------------------------------------
-- news
-- ------------------------------------------------------------
INSERT INTO news (id, author_id, title, body, priority, status, published_at)
SELECT '16111111-1111-4111-8111-111111111111',
       'a2222222-2222-4222-8222-222222222222',
       'August money tip',
       'Track every expense for two weeks. Small leaks become visible quickly.',
       'normal',
       'published',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE id = '16111111-1111-4111-8111-111111111111'
);
