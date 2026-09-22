package com.microvault.service;

import com.microvault.dto.GoalDTO;
import com.microvault.model.Goal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Business operations for goals: validation, status handling and the progress
 * figures shown on the goal cards.
 */
public interface GoalService {

    GoalDTO createGoal(Goal goal);

    GoalDTO getGoalById(UUID id);

    List<GoalDTO> getAllGoals();

    List<GoalDTO> getGoalsByUserId(UUID userId);

    List<GoalDTO> getActiveGoalsByUserId(UUID userId);

    BigDecimal getGoalProgress(UUID goalId);

    boolean updateGoal(Goal goal);

    boolean softDeleteGoal(UUID id);
}
