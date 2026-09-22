package com.microvault.support;

import com.microvault.dao.BudgetDAO;
import com.microvault.model.Budget;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryBudgetDAO implements BudgetDAO {

    private final List<Budget> budgets = new ArrayList<>();

    @Override
    public Budget create(Budget budget) {
        if (budget.getId() == null) {
            budget.setId(UUID.randomUUID());
        }
        budget.setCreatedAt(LocalDateTime.now());
        budget.setUpdatedAt(LocalDateTime.now());
        budget.setIsDeleted(Boolean.FALSE);
        budgets.add(copy(budget));
        return copy(budget);
    }

    @Override
    public Budget findById(UUID id) {
        for (Budget budget : budgets) {
            if (id.equals(budget.getId()) && !Boolean.TRUE.equals(budget.getIsDeleted())) {
                return copy(budget);
            }
        }
        return null;
    }

    @Override
    public List<Budget> findAll() {
        List<Budget> result = new ArrayList<>();
        for (Budget budget : budgets) {
            if (!Boolean.TRUE.equals(budget.getIsDeleted())) {
                result.add(copy(budget));
            }
        }
        return result;
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        List<Budget> result = new ArrayList<>();
        for (Budget budget : budgets) {
            if (!Boolean.TRUE.equals(budget.getIsDeleted()) && userId.equals(budget.getUserId())) {
                result.add(copy(budget));
            }
        }
        return result;
    }

    @Override
    public Budget findByUserIdAndCategory(UUID userId, String category) {
        for (Budget budget : budgets) {
            if (!Boolean.TRUE.equals(budget.getIsDeleted())
                    && userId.equals(budget.getUserId())
                    && category.equals(budget.getCategory())) {
                return copy(budget);
            }
        }
        return null;
    }

    @Override
    public boolean update(Budget budget) {
        Budget existing = findStored(budget.getId());
        if (existing == null) {
            return false;
        }
        existing.setCategory(budget.getCategory());
        existing.setMonthlyLimit(budget.getMonthlyLimit());
        existing.setNote(budget.getNote());
        existing.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        Budget existing = findStored(id);
        if (existing == null) {
            return false;
        }
        existing.setIsDeleted(Boolean.TRUE);
        existing.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private Budget findStored(UUID id) {
        for (Budget budget : budgets) {
            if (id.equals(budget.getId()) && !Boolean.TRUE.equals(budget.getIsDeleted())) {
                return budget;
            }
        }
        return null;
    }

    private Budget copy(Budget source) {
        Budget budget = new Budget();
        budget.setId(source.getId());
        budget.setUserId(source.getUserId());
        budget.setCategory(source.getCategory());
        budget.setMonthlyLimit(source.getMonthlyLimit());
        budget.setNote(source.getNote());
        budget.setCreatedAt(source.getCreatedAt());
        budget.setUpdatedAt(source.getUpdatedAt());
        budget.setIsDeleted(source.getIsDeleted());
        budget.setDeletedAt(source.getDeletedAt());
        return budget;
    }
}
