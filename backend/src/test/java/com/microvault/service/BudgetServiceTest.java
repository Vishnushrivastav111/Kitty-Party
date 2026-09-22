package com.microvault.service;

import com.microvault.dto.BudgetDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Budget;
import com.microvault.serviceimpl.BudgetServiceImpl;
import com.microvault.support.InMemoryBudgetDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {

    private BudgetService budgetService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetServiceImpl(new InMemoryBudgetDAO());
        userId = UUID.randomUUID();
    }

    @Test
    void createValidBudget() {
        BudgetDTO saved = budgetService.createBudget(validBudget("Food", "12000"));

        assertNotNull(saved.getId());
        assertEquals("Food", saved.getCategory());
        assertEquals(new BigDecimal("12000"), saved.getMonthlyLimit());
    }

    @Test
    void calculateBudgetUsage() {
        BudgetDTO saved = budgetService.createBudget(validBudget("Travel", "5000"));

        BigDecimal usage = budgetService.calculateBudgetUtilization(saved.getId(), new BigDecimal("2500"));

        assertEquals(new BigDecimal("50.00"), usage);
        assertFalse(budgetService.isOverBudget(saved.getId(), new BigDecimal("2500")));
    }

    @Test
    void updateBudget() {
        BudgetDTO saved = budgetService.createBudget(validBudget("Bills", "8000"));
        Budget update = validBudget("Bills", "9000");
        update.setId(saved.getId());

        assertTrue(budgetService.updateBudget(update));
        assertEquals(new BigDecimal("9000"), budgetService.getBudgetById(saved.getId()).getMonthlyLimit());
    }

    @Test
    void zeroLimitIsRejected() {
        Budget budget = validBudget("Food", "0");

        assertThrows(ValidationException.class, () -> budgetService.createBudget(budget));
    }

    @Test
    void negativeLimitIsRejected() {
        Budget budget = validBudget("Food", "-10");

        assertThrows(ValidationException.class, () -> budgetService.createBudget(budget));
    }

    @Test
    void missingCategoryIsRejected() {
        Budget budget = validBudget(" ", "1000");
        budget.setCategory(null);

        assertThrows(ValidationException.class, () -> budgetService.createBudget(budget));
    }

    @Test
    void duplicateCategoryIsRejected() {
        budgetService.createBudget(validBudget("Food", "12000"));

        assertThrows(ValidationException.class, () -> budgetService.createBudget(validBudget("Food", "8000")));
    }

    private Budget validBudget(String category, String limit) {
        return new Budget(userId, category, new BigDecimal(limit), "test budget");
    }
}
