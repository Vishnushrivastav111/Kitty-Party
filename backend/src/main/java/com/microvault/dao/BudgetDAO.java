package com.microvault.dao;

import com.microvault.model.Budget;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "budgets" table. Only data access is described
 * here, no business rules.
 */
public interface BudgetDAO {

    Budget create(Budget budget);

    Budget findById(UUID id);

    List<Budget> findAll();

    List<Budget> findByUserId(UUID userId);

    Budget findByUserIdAndCategory(UUID userId, String category);

    boolean update(Budget budget);

    boolean softDelete(UUID id);
}
