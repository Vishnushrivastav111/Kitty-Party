package com.microvault.serviceimpl;

import com.microvault.business.GoalBO;
import com.microvault.dao.GoalDAO;
import com.microvault.dto.GoalDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.Goal;
import com.microvault.service.GoalService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for goals. The DAO is received through the constructor, so
 * this class is bound to the GoalDAO interface and not to a concrete class.
 */
public class GoalServiceImpl implements GoalService {

    private final GoalDAO goalDAO;
    private final GoalBO goalBO = new GoalBO();

    public GoalServiceImpl(GoalDAO goalDAO) {
        this.goalDAO = goalDAO;
    }

    @Override
    public GoalDTO createGoal(Goal goal) {

        validateGoal(goal);

        // A fully funded goal is stored as "completed" right away.
        goal.setStatus(goalBO.decideStatus(goal));

        Goal savedGoal = goalDAO.create(goal);
        return toDTO(savedGoal);
    }

    @Override
    public GoalDTO getGoalById(UUID id) {
        if (id == null) {
            throw new ValidationException("Goal id is required");
        }
        Goal goal = goalDAO.findById(id);
        if (goal == null) {
            throw new ValidationException("No active goal found with id " + id);
        }
        return toDTO(goal);
    }

    @Override
    public List<GoalDTO> getAllGoals() {
        return toDTOList(goalDAO.findAll());
    }

    @Override
    public List<GoalDTO> getGoalsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(goalDAO.findByUserId(userId));
    }

    @Override
    public List<GoalDTO> getActiveGoalsByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(goalDAO.findActiveGoalsByUserId(userId));
    }

    @Override
    public BigDecimal getGoalProgress(UUID goalId) {
        if (goalId == null) {
            throw new ValidationException("Goal id is required");
        }
        Goal goal = goalDAO.findById(goalId);
        if (goal == null) {
            throw new ValidationException("No active goal found with id " + goalId);
        }
        return goalBO.calculateProgressPercentage(goal);
    }

    @Override
    public boolean updateGoal(Goal goal) {

        if (goal == null || goal.getId() == null) {
            throw new ValidationException("Goal id is required for an update");
        }
        validateGoal(goal);

        Goal existingGoal = goalDAO.findById(goal.getId());
        if (existingGoal == null) {
            throw new ValidationException("No active goal found with id " + goal.getId());
        }

        goal.setStatus(goalBO.decideStatus(goal));

        return goalDAO.update(goal);
    }

    @Override
    public boolean softDeleteGoal(UUID id) {
        if (id == null) {
            throw new ValidationException("Goal id is required");
        }
        if (goalDAO.findById(id) == null) {
            throw new ValidationException("No active goal found with id " + id);
        }
        return goalDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateGoal(Goal goal) {

        if (goal == null) {
            throw new ValidationException("Goal is required");
        }
        if (goal.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (goal.getTitle() == null || goal.getTitle().trim().isEmpty()) {
            throw new ValidationException("Goal title is required");
        }
        if (goal.getCategory() == null || goal.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (goal.getTargetAmount() == null) {
            throw new ValidationException("Target amount is required");
        }
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Target amount must be greater than zero");
        }

        // A goal that has not been funded yet simply starts at zero.
        if (goal.getSavedAmount() == null) {
            goal.setSavedAmount(BigDecimal.ZERO);
        }
        if (goal.getSavedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Saved amount cannot be negative");
        }
        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) > 0) {
            throw new ValidationException("Saved amount cannot be greater than the target amount");
        }

        if (goal.getDeadline() == null) {
            throw new ValidationException("Deadline is required");
        }
        if (goal.getDeadline().isBefore(LocalDate.now())) {
            throw new ValidationException("Deadline cannot be in the past");
        }
        if (goal.getStatus() != null && !goal.getStatus().trim().isEmpty()
                && !goal.getStatus().matches("active|completed|paused")) {
            throw new ValidationException("Status must be active, completed or paused");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private GoalDTO toDTO(Goal goal) {

        GoalDTO goalDTO = new GoalDTO();
        goalDTO.setId(goal.getId());
        goalDTO.setUserId(goal.getUserId());
        goalDTO.setTitle(goal.getTitle());
        goalDTO.setCategory(goal.getCategory());
        goalDTO.setTargetAmount(goal.getTargetAmount());
        goalDTO.setSavedAmount(goal.getSavedAmount());
        goalDTO.setDeadline(goal.getDeadline());
        goalDTO.setStatus(goal.getStatus());
        goalDTO.setProgressPercentage(goalBO.calculateProgressPercentage(goal));
        goalDTO.setRemainingAmount(goalBO.calculateRemainingAmount(goal));
        goalDTO.setCreatedAt(goal.getCreatedAt());
        return goalDTO;
    }

    private List<GoalDTO> toDTOList(List<Goal> goals) {
        List<GoalDTO> goalDTOs = new ArrayList<>();
        for (Goal goal : goals) {
            goalDTOs.add(toDTO(goal));
        }
        return goalDTOs;
    }
}
