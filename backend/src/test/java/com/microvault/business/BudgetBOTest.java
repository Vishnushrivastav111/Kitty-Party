package com.microvault.business;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetBOTest {

    private final BudgetBO budgetBO = new BudgetBO();

    @Test
    void calculateBudgetUtilization() {
        BigDecimal usage = budgetBO.calculateUtilizationPercentage(
                new BigDecimal("10000"), new BigDecimal("4000"));

        assertEquals(new BigDecimal("40.00"), usage);
        assertEquals(new BigDecimal("6000"), budgetBO.calculateRemainingAmount(
                new BigDecimal("10000"), new BigDecimal("4000")));
        assertEquals("Within budget", budgetBO.describeUsage(
                new BigDecimal("10000"), new BigDecimal("4000")));
        assertFalse(budgetBO.isOverBudget(new BigDecimal("10000"), new BigDecimal("4000")));
    }

    @Test
    void overBudgetIsDetected() {
        assertTrue(budgetBO.isOverBudget(new BigDecimal("5000"), new BigDecimal("6000")));
        assertEquals("Over budget", budgetBO.describeUsage(new BigDecimal("5000"), new BigDecimal("6000")));
        assertEquals(BigDecimal.ZERO, budgetBO.calculateRemainingAmount(
                new BigDecimal("5000"), new BigDecimal("6000")));
    }

    @Test
    void zeroOrMissingLimitGivesZeroUsage() {
        assertEquals(BigDecimal.ZERO, budgetBO.calculateUtilizationPercentage(BigDecimal.ZERO, new BigDecimal("10")));
        assertEquals(BigDecimal.ZERO, budgetBO.calculateUtilizationPercentage(null, new BigDecimal("10")));
        assertFalse(budgetBO.isOverBudget(null, new BigDecimal("10")));
    }
}
