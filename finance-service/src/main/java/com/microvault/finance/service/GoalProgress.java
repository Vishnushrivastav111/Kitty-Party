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
                total = total.add(entry.getAmount());
            }
        }
        return total;
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
