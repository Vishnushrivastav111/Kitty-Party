package com.microvault.dao;

import com.microvault.model.Goal;

import java.util.List;
import java.util.UUID;

/**
 * Database operations for the "goals" table. Only data access is described
 * here, no business rules.
 */
public interface GoalDAO {

    Goal create(Goal goal);

    Goal findById(UUID id);

    List<Goal> findAll();

    List<Goal> findByUserId(UUID userId);

    List<Goal> findActiveGoalsByUserId(UUID userId);

    boolean update(Goal goal);

    boolean softDelete(UUID id);
}
