-- Additive migration for existing MicroVault databases.
-- Safe to re-run. Does not drop tables or delete user rows.
-- Hibernate ddl-auto is none, so apply this script before starting the updated services.

ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS additional_income NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS other_commitments NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE finance_profiles ADD COLUMN IF NOT EXISTS emergency_reserve NUMERIC(14,2) NOT NULL DEFAULT 0;

ALTER TABLE goals ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE goals ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'medium';
ALTER TABLE goals ADD COLUMN IF NOT EXISTS planned_contribution NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE goals ADD COLUMN IF NOT EXISTS opening_saved NUMERIC(14,2) NOT NULL DEFAULT 0;

UPDATE goals
   SET opening_saved = saved_amount
 WHERE opening_saved = 0
   AND saved_amount > 0;

ALTER TABLE goals DROP CONSTRAINT IF EXISTS chk_goals_status;
ALTER TABLE goals ADD CONSTRAINT chk_goals_status
    CHECK (status IN ('draft', 'active', 'paused', 'completed', 'archived'));

ALTER TABLE savings_entries ADD COLUMN IF NOT EXISTS goal_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_savings_entries_goal'
    ) THEN
        ALTER TABLE savings_entries
            ADD CONSTRAINT fk_savings_entries_goal
            FOREIGN KEY (goal_id) REFERENCES goals (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_savings_entries_goal ON savings_entries (goal_id);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_key VARCHAR(200);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dismissed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS budget_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_reference_active
    ON notifications (user_id, reference_key)
    WHERE reference_key IS NOT NULL AND is_deleted = FALSE;

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS chk_transactions_type;
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_type
    CHECK (type IN ('income', 'expense', 'refund', 'transfer'));

ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS emi_months INTEGER;
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS monthly_emi NUMERIC(14,2);
ALTER TABLE affordability_checks ADD COLUMN IF NOT EXISTS total_repayment NUMERIC(14,2);
