package com.microvault.business;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Budget calculations: how much of a monthly limit has been used.
 */
public class BudgetBO {

    /** Used percentage of the monthly limit, rounded to two decimals. */
    public BigDecimal calculateUtilizationPercentage(BigDecimal monthlyLimit, BigDecimal spentAmount) {

        if (monthlyLimit == null || monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal spent = spentAmount == null ? BigDecimal.ZERO : spentAmount;
        return spent.multiply(new BigDecimal("100"))
                .divide(monthlyLimit, 2, RoundingMode.HALF_UP);
    }

    /** Money left in the budget; never negative. */
    public BigDecimal calculateRemainingAmount(BigDecimal monthlyLimit, BigDecimal spentAmount) {

        if (monthlyLimit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal spent = spentAmount == null ? BigDecimal.ZERO : spentAmount;
        BigDecimal remaining = monthlyLimit.subtract(spent);

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return remaining;
    }

    /** True when spending crossed the limit. */
    public boolean isOverBudget(BigDecimal monthlyLimit, BigDecimal spentAmount) {
        if (monthlyLimit == null || spentAmount == null) {
            return false;
        }
        return spentAmount.compareTo(monthlyLimit) > 0;
    }

    /** Simple label used by the dashboard cards. */
    public String describeUsage(BigDecimal monthlyLimit, BigDecimal spentAmount) {

        BigDecimal usage = calculateUtilizationPercentage(monthlyLimit, spentAmount);

        if (usage.compareTo(new BigDecimal("100")) > 0) {
            return "Over budget";
        }
        if (usage.compareTo(new BigDecimal("80")) >= 0) {
            return "Close to limit";
        }
        return "Within budget";
    }
}
