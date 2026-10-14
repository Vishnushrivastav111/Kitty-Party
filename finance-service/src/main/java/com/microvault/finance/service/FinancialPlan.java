package com.microvault.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Single definition of MicroVault's monthly plan.
 *
 * <p>Total monthly income is take-home pay plus other recurring income.
 * Savings balances and investments are stocks, not income.
 *
 * <p>Essential expenses, EMI, and other commitments are the recurring
 * obligations captured in financial setup. They are estimates of obligations,
 * not a second copy of expense transactions.
 *
 * <p>Planning capacity = total monthly income − essential expenses − EMI − other commitments.
 * Budget capacity = planning capacity − planned contributions of active goals.
 * Remaining budget capacity = budget capacity − category limits already allocated.
 * Goal contribution capacity = planning capacity − category limits − other active goal plans.
 *
 * <p>Category budget spending uses expense transactions in that category for the
 * current calendar month, minus refunds. Transfers, savings, and investments
 * are not consumption. Amounts use scale 2, half up.
 */
public final class FinancialPlan {

    public static final int MAX_ACTIVE_GOALS = 3;
    public static final int[] BUDGET_THRESHOLDS = {25, 50, 80, 100};

    private FinancialPlan() {
    }

    public static BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalMonthlyIncome(BigDecimal takeHome, BigDecimal additionalIncome) {
        return money(takeHome).add(money(additionalIncome));
    }

    public static BigDecimal planningCapacity(BigDecimal totalIncome, BigDecimal essentialExpenses,
                                              BigDecimal emi, BigDecimal otherCommitments) {
        return money(totalIncome)
                .subtract(money(essentialExpenses))
                .subtract(money(emi))
                .subtract(money(otherCommitments));
    }

    public static BigDecimal budgetCapacity(BigDecimal planningCapacity, BigDecimal activeGoalContributions) {
        return money(planningCapacity).subtract(money(activeGoalContributions));
    }

    public static BigDecimal remainingBudgetCapacity(BigDecimal budgetCapacity, BigDecimal allocatedLimits) {
        return money(budgetCapacity).subtract(money(allocatedLimits));
    }

    public static BigDecimal goalContributionCapacity(BigDecimal planningCapacity, BigDecimal allocatedLimits,
                                                      BigDecimal otherActiveGoalContributions) {
        return money(planningCapacity)
                .subtract(money(allocatedLimits))
                .subtract(money(otherActiveGoalContributions));
    }

    public static BigDecimal nonNegative(BigDecimal amount) {
        BigDecimal value = money(amount);
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }

    /**
     * Contribution periods are whole months from the current month to the deadline month.
     * The current month counts as one period when the deadline is still today or later.
     * A deadline before today has no remaining periods.
     */
    public static int contributionPeriods(LocalDate today, LocalDate deadline) {
        if (today == null || deadline == null || deadline.isBefore(today)) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline));
        if (months < 1) {
            return 1;
        }
        return (int) months;
    }

    public static BigDecimal requiredMonthly(BigDecimal remaining, int periods) {
        if (periods <= 0) {
            return null;
        }
        BigDecimal left = money(remaining);
        if (left.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return left.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal monthlyInstallment(BigDecimal principal, int months, BigDecimal annualPercent) {
        BigDecimal amount = money(principal);
        if (months <= 0 || amount.signum() <= 0) {
            return null;
        }
        if (annualPercent == null || annualPercent.signum() == 0) {
            return amount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualPercent.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        return amount.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    public static boolean nonConsumptionCategory(String category) {
        if (category == null) {
            return false;
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        return "transfer".equals(value) || "savings".equals(value) || "investment".equals(value);
    }

    public static boolean openingMirror(String category) {
        return category != null && "opening".equalsIgnoreCase(category.trim());
    }

    public static boolean emergencyCategory(String category) {
        return category != null && "emergency".equalsIgnoreCase(category.trim());
    }

    public static String rupee(BigDecimal amount) {
        return "₹" + money(amount).toPlainString();
    }

    public static String budgetCapacityMessage(BigDecimal capacity, BigDecimal allocated, BigDecimal remaining) {
        BigDecimal room = money(remaining);
        if (room.signum() < 0) {
            room = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return "Your available budget capacity is " + rupee(capacity)
                + ". You have already allocated " + rupee(allocated)
                + ", so you can allocate up to " + rupee(room) + " more.";
    }
}
