package com.microvault.service;

import com.microvault.dto.BudgetDTO;
import com.microvault.model.Budget;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for budgets: validation, lookups and the utilisation
 * figures shown on the budget cards.
 */
public interface BudgetService {

    BudgetDTO createBudget(Budget budget);

    BudgetDTO getBudgetById(UUID id);

    List<BudgetDTO> getAllBudgets();

    List<BudgetDTO> getBudgetsByUserId(UUID userId);

    BigDecimal calculateBudgetUtilization(UUID budgetId, BigDecimal spentAmount);

    boolean isOverBudget(UUID budgetId, BigDecimal spentAmount);

    boolean updateBudget(Budget budget);

    boolean softDeleteBudget(UUID id);
}
