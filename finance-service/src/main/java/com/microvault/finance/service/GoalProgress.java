package com.microvault.finance.service;

import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Goal progress is opening savings plus later contributions linked to that goal. */
public final class GoalProgress {

    private GoalProgress() {
    }

    public static BigDecimal contributed(List<SavingsEntry> entries, UUID goalId) {
        BigDecimal total = BigDecimal.ZERO;
        if (entries == null || goalId == null) {
            return total;
        }
        for (SavingsEntry entry : entries) {
            if (entry.getGoalId() != null && goalId.equals(entry.getGoalId()) && entry.getAmount() != null) {
                total = total.add(signed(entry));
            }
        }
        return total;
    }

    /** A withdrawal linked to a goal takes money out of that goal; anything else adds to it. */
    public static BigDecimal signed(SavingsEntry entry) {
        if (entry == null || entry.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = entry.getAmount().abs();
        return FinancialPlan.withdrawalCategory(entry.getCategory()) ? amount.negate() : amount;
    }

    public static BigDecimal opening(Goal goal) {
        if (goal.getOpeningSaved() != null && goal.getOpeningSaved().signum() > 0) {
            return goal.getOpeningSaved();
        }
        if (goal.getSaved() != null) {
            return goal.getSaved();
        }
        return BigDecimal.ZERO;
    }
}
