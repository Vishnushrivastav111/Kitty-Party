package com.microvault.service;

import com.microvault.dto.GoalDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Goal;
import com.microvault.serviceimpl.GoalServiceImpl;
import com.microvault.support.InMemoryGoalDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalServiceTest {

    private GoalService goalService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(new InMemoryGoalDAO());
        userId = UUID.randomUUID();
    }

    @Test
    void createValidGoal() {
        GoalDTO saved = goalService.createGoal(validGoal("Emergency fund", "200000", "50000"));

        assertNotNull(saved.getId());
        assertEquals("Emergency fund", saved.getTitle());
        assertEquals(new BigDecimal("25.00"), saved.getProgressPercentage());
    }

    @Test
    void calculateProgress() {
        GoalDTO saved = goalService.createGoal(validGoal("Vacation", "100000", "40000"));

        BigDecimal progress = goalService.getGoalProgress(saved.getId());

        assertEquals(new BigDecimal("40.00"), progress);
    }

    @Test
    void updateGoal() {
        GoalDTO saved = goalService.createGoal(validGoal("Laptop", "80000", "10000"));
        Goal update = validGoal("Work laptop", "80000", "20000");
        update.setId(saved.getId());

        assertTrue(goalService.updateGoal(update));
        assertEquals("Work laptop", goalService.getGoalById(saved.getId()).getTitle());
        assertEquals(new BigDecimal("25.00"), goalService.getGoalProgress(saved.getId()));
    }

    @Test
    void zeroTargetIsRejected() {
        Goal goal = validGoal("Zero", "0", "0");

        assertThrows(ValidationException.class, () -> goalService.createGoal(goal));
    }

    @Test
    void negativeTargetIsRejected() {
        Goal goal = validGoal("Negative", "-100", "0");

        assertThrows(ValidationException.class, () -> goalService.createGoal(goal));
    }

    @Test
    void savedAmountGreaterThanTargetIsRejected() {
        Goal goal = validGoal("Overflow", "1000", "1500");

        assertThrows(ValidationException.class, () -> goalService.createGoal(goal));
    }

    @Test
    void invalidDeadlineIsRejected() {
        Goal goal = validGoal("Past deadline", "1000", "100");
        goal.setDeadline(LocalDate.now().minusDays(1));

        assertThrows(ValidationException.class, () -> goalService.createGoal(goal));
    }

    @Test
    void missingGoalIsNotFound() {
        assertThrows(ValidationException.class, () -> goalService.getGoalById(UUID.randomUUID()));
    }

    private Goal validGoal(String title, String target, String saved) {
        return new Goal(userId, title, "Savings", new BigDecimal(target), new BigDecimal(saved),
                LocalDate.now().plusMonths(6), "active");
    }
}
