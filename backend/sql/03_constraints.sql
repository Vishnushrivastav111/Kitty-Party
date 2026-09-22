-- ============================================================
-- MicroVault - 03_constraints.sql
-- Unique keys, foreign keys and check constraints.
-- Run after 02_create_tables.sql.
-- ============================================================

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('user', 'admin', 'superadmin'));

ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN ('active', 'inactive'));

ALTER TABLE users ADD CONSTRAINT chk_users_phone
    CHECK (phone ~ '^[0-9]{10}$');


-- ------------------------------------------------------------
-- finance_profiles
-- ------------------------------------------------------------
ALTER TABLE finance_profiles ADD CONSTRAINT fk_finance_profiles_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE finance_profiles ADD CONSTRAINT chk_finance_profiles_income
    CHECK (monthly_income >= 0);

ALTER TABLE finance_profiles ADD CONSTRAINT chk_finance_profiles_expenses
    CHECK (monthly_expenses >= 0);

ALTER TABLE finance_profiles ADD CONSTRAINT chk_finance_profiles_savings
    CHECK (current_savings >= 0);

ALTER TABLE finance_profiles ADD CONSTRAINT chk_finance_profiles_budget
    CHECK (monthly_budget >= 0);


-- ------------------------------------------------------------
-- transactions
-- ------------------------------------------------------------
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions ADD CONSTRAINT chk_transactions_type
    CHECK (type IN ('income', 'expense'));

ALTER TABLE transactions ADD CONSTRAINT chk_transactions_amount
    CHECK (amount > 0);


-- ------------------------------------------------------------
-- goals
-- ------------------------------------------------------------
ALTER TABLE goals ADD CONSTRAINT fk_goals_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE goals ADD CONSTRAINT chk_goals_target
    CHECK (target_amount > 0);

ALTER TABLE goals ADD CONSTRAINT chk_goals_saved
    CHECK (saved_amount >= 0 AND saved_amount <= target_amount);

ALTER TABLE goals ADD CONSTRAINT chk_goals_status
    CHECK (status IN ('active', 'completed', 'paused'));


-- ------------------------------------------------------------
-- savings_entries
-- ------------------------------------------------------------
ALTER TABLE savings_entries ADD CONSTRAINT fk_savings_entries_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE savings_entries ADD CONSTRAINT chk_savings_entries_amount
    CHECK (amount > 0);


-- ------------------------------------------------------------
-- budgets
-- ------------------------------------------------------------
ALTER TABLE budgets ADD CONSTRAINT fk_budgets_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE budgets ADD CONSTRAINT chk_budgets_limit
    CHECK (monthly_limit > 0);

-- One active budget per category per user (deleted rows are ignored).
CREATE UNIQUE INDEX uq_budgets_user_category_active
    ON budgets (user_id, category)
    WHERE is_deleted = FALSE;


-- ------------------------------------------------------------
-- affordability_checks
-- ------------------------------------------------------------
ALTER TABLE affordability_checks ADD CONSTRAINT fk_affordability_checks_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE affordability_checks ADD CONSTRAINT chk_affordability_checks_amount
    CHECK (amount > 0);

ALTER TABLE affordability_checks ADD CONSTRAINT chk_affordability_checks_level
    CHECK (level IS NULL OR level IN ('success', 'warning', 'danger'));


-- ------------------------------------------------------------
-- notifications
-- ------------------------------------------------------------
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('info', 'success', 'warning', 'danger'));


-- ------------------------------------------------------------
-- reports
-- ------------------------------------------------------------
ALTER TABLE reports ADD CONSTRAINT fk_reports_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE reports ADD CONSTRAINT chk_reports_date_range
    CHECK (from_date <= to_date);

ALTER TABLE reports ADD CONSTRAINT chk_reports_counts
    CHECK (transaction_count >= 0);


-- ------------------------------------------------------------
-- feedback
-- ------------------------------------------------------------
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE feedback ADD CONSTRAINT chk_feedback_status
    CHECK (status IN ('open', 'in-review', 'resolved', 'closed'));


-- ------------------------------------------------------------
-- feedback_history
-- ------------------------------------------------------------
ALTER TABLE feedback_history ADD CONSTRAINT fk_feedback_history_feedback
    FOREIGN KEY (feedback_id) REFERENCES feedback (id);

ALTER TABLE feedback_history ADD CONSTRAINT fk_feedback_history_user
    FOREIGN KEY (changed_by) REFERENCES users (id);


-- ------------------------------------------------------------
-- news
-- ------------------------------------------------------------
ALTER TABLE news ADD CONSTRAINT fk_news_author
    FOREIGN KEY (author_id) REFERENCES users (id);

ALTER TABLE news ADD CONSTRAINT chk_news_status
    CHECK (status IN ('draft', 'published', 'archived'));

ALTER TABLE news ADD CONSTRAINT chk_news_priority
    CHECK (priority IN ('low', 'normal', 'high'));
