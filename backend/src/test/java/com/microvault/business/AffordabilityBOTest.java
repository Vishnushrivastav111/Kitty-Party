package com.microvault.business;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffordabilityBOTest {

    private final AffordabilityBO affordabilityBO = new AffordabilityBO();

    @Test
    void validAffordabilityCalculation() {
        BigDecimal available = affordabilityBO.calculateAvailableCapacity(
                new BigDecimal("80000"),
                new BigDecimal("40000"),
                new BigDecimal("20000")
        );

        assertEquals(new BigDecimal("46000.00"), available);
        assertEquals("Comfortably affordable", affordabilityBO.decideVerdict(new BigDecimal("10000"), available));
        assertEquals("success", affordabilityBO.decideLevel(new BigDecimal("10000"), available));
        assertTrue(affordabilityBO.isAffordable(new BigDecimal("10000"), available));
    }

    @Test
    void expensiveItemIsNotRecommended() {
        BigDecimal available = affordabilityBO.calculateAvailableCapacity(
                new BigDecimal("30000"),
                new BigDecimal("25000"),
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("5000.00"), available);
        assertEquals("Not recommended", affordabilityBO.decideVerdict(new BigDecimal("20000"), available));
        assertEquals("danger", affordabilityBO.decideLevel(new BigDecimal("20000"), available));
        assertFalse(affordabilityBO.isAffordable(new BigDecimal("20000"), available));
    }

    @Test
    void negativeOrMissingInputsAreTreatedAsZero() {
        BigDecimal available = affordabilityBO.calculateAvailableCapacity(
                new BigDecimal("10000"),
                new BigDecimal("15000"),
                null
        );

        assertEquals(new BigDecimal("0.00"), available);
        assertFalse(affordabilityBO.isAffordable(null, available));
    }
}
