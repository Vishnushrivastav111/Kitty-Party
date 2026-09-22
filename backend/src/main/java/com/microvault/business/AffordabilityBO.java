package com.microvault.business;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Works out whether a planned purchase fits into the money a member has left.
 *
 *   available capacity = (monthly income - monthly expenses) + 30% of savings
 *
 * The 30% share of savings mirrors the rule the existing frontend uses, so the
 * verdict shown on the Affordability page stays the same.
 */
public class AffordabilityBO {

    private static final BigDecimal SAVINGS_SHARE = new BigDecimal("0.30");

    public BigDecimal calculateAvailableCapacity(BigDecimal monthlyIncome,
                                                 BigDecimal monthlyExpenses,
                                                 BigDecimal totalSavings) {

        BigDecimal income = monthlyIncome == null ? BigDecimal.ZERO : monthlyIncome;
        BigDecimal expenses = monthlyExpenses == null ? BigDecimal.ZERO : monthlyExpenses;
        BigDecimal savings = totalSavings == null ? BigDecimal.ZERO : totalSavings;

        BigDecimal monthlySurplus = income.subtract(expenses);
        if (monthlySurplus.compareTo(BigDecimal.ZERO) < 0) {
            monthlySurplus = BigDecimal.ZERO;
        }

        BigDecimal usableSavings = savings.multiply(SAVINGS_SHARE);
        return monthlySurplus.add(usableSavings).setScale(2, RoundingMode.HALF_UP);
    }

    /** Share of the available capacity the purchase would eat up. */
    public BigDecimal calculateSpendRatio(BigDecimal amount, BigDecimal availableCapacity) {

        if (availableCapacity == null || availableCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal purchaseAmount = amount == null ? BigDecimal.ZERO : amount;
        return purchaseAmount.divide(availableCapacity, 4, RoundingMode.HALF_UP);
    }

    /** Text shown to the member. */
    public String decideVerdict(BigDecimal amount, BigDecimal availableCapacity) {

        BigDecimal ratio = calculateSpendRatio(amount, availableCapacity);

        if (ratio.compareTo(new BigDecimal("0.30")) <= 0) {
            return "Comfortably affordable";
        }
        if (ratio.compareTo(new BigDecimal("0.60")) <= 0) {
            return "Affordable with caution";
        }
        if (ratio.compareTo(BigDecimal.ONE) <= 0) {
            return "Tight - consider delaying";
        }
        return "Not recommended";
    }

    /** Colour level used by the UI badge: success, warning or danger. */
    public String decideLevel(BigDecimal amount, BigDecimal availableCapacity) {

        BigDecimal ratio = calculateSpendRatio(amount, availableCapacity);

        if (ratio.compareTo(new BigDecimal("0.30")) <= 0) {
            return "success";
        }
        if (ratio.compareTo(BigDecimal.ONE) <= 0) {
            return "warning";
        }
        return "danger";
    }

    public boolean isAffordable(BigDecimal amount, BigDecimal availableCapacity) {
        if (amount == null || availableCapacity == null) {
            return false;
        }
        return amount.compareTo(availableCapacity) <= 0;
    }
}
