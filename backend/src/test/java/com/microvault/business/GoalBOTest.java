package com.microvault.business;

import com.microvault.model.Goal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalBOTest {

    private final GoalBO goalBO = new GoalBO();

    @Test
    void calculateProgressForPartiallyFundedGoal() {
        Goal goal = sampleGoal("100000", "25000");

        assertEquals(new BigDecimal("25.00"), goalBO.calculateProgressPercentage(goal));
        assertEquals(new BigDecimal("75000"), goalBO.calculateRemainingAmount(goal));
        assertFalse(goalBO.isGoalAchieved(goal));
        assertEquals("active", goalBO.decideStatus(goal));
    }

    @Test
    void completedGoalHasFullProgress() {
        Goal goal = sampleGoal("50000", "50000");

        assertEquals(new BigDecimal("100.00"), goalBO.calculateProgressPercentage(goal));
        assertEquals(BigDecimal.ZERO, goalBO.calculateRemainingAmount(goal));
        assertTrue(goalBO.isGoalAchieved(goal));
        assertEquals("completed", goalBO.decideStatus(goal));
    }

    @Test
    void zeroOrMissingTargetGivesZeroProgress() {
        Goal goal = sampleGoal("0", "10");

        assertEquals(BigDecimal.ZERO, goalBO.calculateProgressPercentage(goal));
        assertEquals(0, goalBO.calculateDaysLeft(null, LocalDate.now()));
    }

    private Goal sampleGoal(String target, String saved) {
        return new Goal(UUID.randomUUID(), "Emergency fund", "Savings",
                new BigDecimal(target), new BigDecimal(saved), LocalDate.now().plusDays(30), "active");
    }
}
