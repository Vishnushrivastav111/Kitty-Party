-- ============================================================
-- MicroVault - 04_indexes.sql
-- Indexes for the columns the application actually searches on.
-- ============================================================

-- users: login by email, admin screens filter by role/status
CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- finance_profiles: always loaded for the logged-in user
CREATE INDEX idx_finance_profiles_user ON finance_profiles (user_id);

-- transactions: listed per user, filtered by date and category
CREATE INDEX idx_transactions_user     ON transactions (user_id);
CREATE INDEX idx_transactions_date     ON transactions (transaction_date);
CREATE INDEX idx_transactions_category ON transactions (category);

-- goals
CREATE INDEX idx_goals_user   ON goals (user_id);
CREATE INDEX idx_goals_status ON goals (status);

-- savings_entries
CREATE INDEX idx_savings_entries_user ON savings_entries (user_id);
CREATE INDEX idx_savings_entries_date ON savings_entries (entry_date);

-- budgets
CREATE INDEX idx_budgets_user ON budgets (user_id);

-- affordability_checks
CREATE INDEX idx_affordability_checks_user ON affordability_checks (user_id);

-- notifications: inbox and unread badge
CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_read ON notifications (is_read);

-- reports
CREATE INDEX idx_reports_user ON reports (user_id);
CREATE INDEX idx_reports_from_date ON reports (from_date);

-- feedback
CREATE INDEX idx_feedback_user   ON feedback (user_id);
CREATE INDEX idx_feedback_status ON feedback (status);

-- feedback_history
CREATE INDEX idx_feedback_history_feedback ON feedback_history (feedback_id);

-- news
CREATE INDEX idx_news_status ON news (status);
CREATE INDEX idx_news_author ON news (author_id);
