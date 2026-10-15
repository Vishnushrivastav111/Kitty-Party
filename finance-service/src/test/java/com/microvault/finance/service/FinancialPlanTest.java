package com.microvault.finance.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialPlanTest {

    @Test
    void incomeDoesNotIncludeSavings() {
        assertEquals(new BigDecimal("55000.00"),
                FinancialPlan.totalMonthlyIncome(new BigDecimal("50000"), new BigDecimal("5000")));
    }

    @Test
    void capacityComesFromIncomeBudgetsAndGoalsOnly() {
        BigDecimal income = new BigDecimal("80000");
        assertEquals(new BigDecimal("65000.00"),
                FinancialPlan.budgetCapacity(income, new BigDecimal("15000")));
        assertEquals(new BigDecimal("10000.00"),
                FinancialPlan.remainingBudgetCapacity(new BigDecimal("25000"), new BigDecimal("15000")));
        // income 80000, budgets 50000, another goal already takes 5000 -> 25000 left for this goal
        assertEquals(new BigDecimal("25000.00"),
                FinancialPlan.goalContributionCapacity(income, new BigDecimal("50000"), new BigDecimal("5000")));
    }

    @Test
    void overCommittedCapacityStaysSignedSoItCanBeReported() {
        assertEquals(new BigDecimal("-4000.00"),
                FinancialPlan.budgetCapacity(new BigDecimal("1000"), new BigDecimal("5000")));
        assertEquals(new BigDecimal("0.00"),
                FinancialPlan.nonNegative(FinancialPlan.goalContributionCapacity(
                        new BigDecimal("1000"), new BigDecimal("900"), new BigDecimal("500"))));
    }

    @Test
    void currentMonthCountsAsOnePeriodAndAPastDeadlineHasNone() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        assertEquals(8, FinancialPlan.contributionPeriods(today, LocalDate.of(2027, 6, 14)));
        assertEquals(1, FinancialPlan.contributionPeriods(today, LocalDate.of(2026, 10, 30)));
        assertEquals(0, FinancialPlan.contributionPeriods(today, LocalDate.of(2026, 10, 1)));
        assertEquals(new BigDecimal("10000.00"),
                FinancialPlan.requiredMonthly(new BigDecimal("80000"), 8));
        assertNull(FinancialPlan.requiredMonthly(new BigDecimal("80000"), 0));
    }

    @Test
    void capacityMessageUsesTheAmountsGiven() {
        String message = FinancialPlan.budgetCapacityMessage(
                new BigDecimal("15000"), new BigDecimal("12000"), new BigDecimal("3000"));
        assertTrue(message.contains("₹15000.00"));
        assertTrue(message.contains("₹12000.00"));
        assertTrue(message.contains("₹3000.00"));
    }

    @Test
    void zeroInterestEmiIsPrincipalDividedByMonths() {
        assertEquals(new BigDecimal("5000.00"),
                FinancialPlan.monthlyInstallment(new BigDecimal("60000"), 12, BigDecimal.ZERO));
    }
}
