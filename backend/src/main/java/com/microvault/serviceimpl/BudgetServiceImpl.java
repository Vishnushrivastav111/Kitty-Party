package com.microvault.serviceimpl;

import com.microvault.business.BudgetBO;
import com.microvault.dao.BudgetDAO;
import com.microvault.dto.BudgetDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Budget;
import com.microvault.service.BudgetService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for budgets. The DAO is received through the constructor, so
 * this class is bound to the BudgetDAO interface and not to a concrete class.
 */
public class BudgetServiceImpl implements BudgetService {

    private final BudgetDAO budgetDAO;
    private final BudgetBO budgetBO = new BudgetBO();

    public BudgetServiceImpl(BudgetDAO budgetDAO) {
        this.budgetDAO = budgetDAO;
    }

    @Override
    public BudgetDTO createBudget(Budget budget) {

        validateBudget(budget);

        // One category can only have one active budget per user.
        if (budgetDAO.findByUserIdAndCategory(budget.getUserId(), budget.getCategory()) != null) {
            throw new ValidationException("A budget for this category already exists");
        }

        Budget savedBudget = budgetDAO.create(budget);
        return toDTO(savedBudget);
    }

    @Override
    public BudgetDTO getBudgetById(UUID id) {
        if (id == null) {
            throw new ValidationException("Budget id is required");
        }
        Budget budget = budgetDAO.findById(id);
        if (budget == null) {
            throw new ValidationException("No active budget found with id " + id);
        }
        return toDTO(budget);
    }

    @Override
    public List<BudgetDTO> getAllBudgets() {
        return toDTOList(budgetDAO.findAll());
    }

    @Override
    public List<BudgetDTO> getBudgetsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(budgetDAO.findByUserId(userId));
    }

    @Override
    public BigDecimal calculateBudgetUtilization(UUID budgetId, BigDecimal spentAmount) {

        if (budgetId == null) {
            throw new ValidationException("Budget id is required");
        }

        Budget budget = budgetDAO.findById(budgetId);
        if (budget == null) {
            throw new ValidationException("No active budget found with id " + budgetId);
        }

        return budgetBO.calculateUtilizationPercentage(budget.getMonthlyLimit(), spentAmount);
    }

    @Override
    public boolean isOverBudget(UUID budgetId, BigDecimal spentAmount) {

        if (budgetId == null) {
            throw new ValidationException("Budget id is required");
        }

        Budget budget = budgetDAO.findById(budgetId);
        if (budget == null) {
            throw new ValidationException("No active budget found with id " + budgetId);
        }

        return budgetBO.isOverBudget(budget.getMonthlyLimit(), spentAmount);
    }

    @Override
    public boolean updateBudget(Budget budget) {

        if (budget == null || budget.getId() == null) {
            throw new ValidationException("Budget id is required for an update");
        }
        validateBudget(budget);

        Budget existingBudget = budgetDAO.findById(budget.getId());
        if (existingBudget == null) {
            throw new ValidationException("No active budget found with id " + budget.getId());
        }

        return budgetDAO.update(budget);
    }

    @Override
    public boolean softDeleteBudget(UUID id) {
        if (id == null) {
            throw new ValidationException("Budget id is required");
        }
        if (budgetDAO.findById(id) == null) {
            throw new ValidationException("No active budget found with id " + id);
        }
        return budgetDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateBudget(Budget budget) {

        if (budget == null) {
            throw new ValidationException("Budget is required");
        }
        if (budget.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (budget.getCategory() == null || budget.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (budget.getMonthlyLimit() == null) {
            throw new ValidationException("Budget limit is required");
        }
        if (budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Budget limit must be greater than zero");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private BudgetDTO toDTO(Budget budget) {

        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setId(budget.getId());
        budgetDTO.setUserId(budget.getUserId());
        budgetDTO.setCategory(budget.getCategory());
        budgetDTO.setMonthlyLimit(budget.getMonthlyLimit());
        budgetDTO.setNote(budget.getNote());
        budgetDTO.setCreatedAt(budget.getCreatedAt());
        return budgetDTO;
    }

    private List<BudgetDTO> toDTOList(List<Budget> budgets) {
        List<BudgetDTO> budgetDTOs = new ArrayList<>();
        for (Budget budget : budgets) {
            budgetDTOs.add(toDTO(budget));
        }
        return budgetDTOs;
    }
}
