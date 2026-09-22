package com.microvault.business;

import com.microvault.model.Goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Goal calculations: progress, remaining amount and the status a goal should
 * have. Keeping this out of the DAO keeps database code free of business rules.
 */
public class GoalBO {

    /** Progress in percent, rounded to two decimals and capped at 100. */
    public BigDecimal calculateProgressPercentage(Goal goal) {

        if (goal == null || goal.getTargetAmount() == null
                || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal saved = goal.getSavedAmount() == null ? BigDecimal.ZERO : goal.getSavedAmount();
        BigDecimal progress = saved.multiply(new BigDecimal("100"))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);

        if (progress.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100.00");
        }
        return progress;
    }

    /** How much money is still needed to reach the goal. */
    public BigDecimal calculateRemainingAmount(Goal goal) {

        if (goal == null || goal.getTargetAmount() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal saved = goal.getSavedAmount() == null ? BigDecimal.ZERO : goal.getSavedAmount();
        BigDecimal remaining = goal.getTargetAmount().subtract(saved);

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return remaining;
    }

    /** True when the saved amount has reached the target. */
    public boolean isGoalAchieved(Goal goal) {
        if (goal == null || goal.getTargetAmount() == null || goal.getSavedAmount() == null) {
            return false;
        }
        return goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0;
    }

    /** A goal that is fully funded becomes "completed", otherwise it stays as it is. */
    public String decideStatus(Goal goal) {
        if (isGoalAchieved(goal)) {
            return "completed";
        }
        if (goal == null || goal.getStatus() == null || goal.getStatus().trim().isEmpty()) {
            return "active";
        }
        return goal.getStatus();
    }

    /** Days left until the deadline. A passed deadline gives a negative number. */
    public long calculateDaysLeft(Goal goal, LocalDate today) {
        if (goal == null || goal.getDeadline() == null || today == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(today, goal.getDeadline());
    }
}
