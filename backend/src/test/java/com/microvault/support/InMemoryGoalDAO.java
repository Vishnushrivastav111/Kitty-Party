package com.microvault.support;

import com.microvault.dao.GoalDAO;
import com.microvault.model.Goal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryGoalDAO implements GoalDAO {

    private final List<Goal> goals = new ArrayList<>();

    @Override
    public Goal create(Goal goal) {
        if (goal.getId() == null) {
            goal.setId(UUID.randomUUID());
        }
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        goal.setIsDeleted(Boolean.FALSE);
        goals.add(copy(goal));
        return copy(goal);
    }

    @Override
    public Goal findById(UUID id) {
        for (Goal goal : goals) {
            if (id.equals(goal.getId()) && !Boolean.TRUE.equals(goal.getIsDeleted())) {
                return copy(goal);
            }
        }
        return null;
    }

    @Override
    public List<Goal> findAll() {
        return findByUser(null, false);
    }

    @Override
    public List<Goal> findByUserId(UUID userId) {
        return findByUser(userId, false);
    }

    @Override
    public List<Goal> findActiveGoalsByUserId(UUID userId) {
        return findByUser(userId, true);
    }

    @Override
    public boolean update(Goal goal) {
        Goal existing = findStored(goal.getId());
        if (existing == null) {
            return false;
        }
        existing.setTitle(goal.getTitle());
        existing.setCategory(goal.getCategory());
        existing.setTargetAmount(goal.getTargetAmount());
        existing.setSavedAmount(goal.getSavedAmount());
        existing.setDeadline(goal.getDeadline());
        existing.setStatus(goal.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        Goal existing = findStored(id);
        if (existing == null) {
            return false;
        }
        existing.setIsDeleted(Boolean.TRUE);
        existing.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private List<Goal> findByUser(UUID userId, boolean activeOnly) {
        List<Goal> result = new ArrayList<>();
        for (Goal goal : goals) {
            if (Boolean.TRUE.equals(goal.getIsDeleted())) {
                continue;
            }
            if (userId != null && !userId.equals(goal.getUserId())) {
                continue;
            }
            if (activeOnly && !"active".equals(goal.getStatus())) {
                continue;
            }
            result.add(copy(goal));
        }
        return result;
    }

    private Goal findStored(UUID id) {
        for (Goal goal : goals) {
            if (id.equals(goal.getId()) && !Boolean.TRUE.equals(goal.getIsDeleted())) {
                return goal;
            }
        }
        return null;
    }

    private Goal copy(Goal source) {
        Goal goal = new Goal();
        goal.setId(source.getId());
        goal.setUserId(source.getUserId());
        goal.setTitle(source.getTitle());
        goal.setCategory(source.getCategory());
        goal.setTargetAmount(source.getTargetAmount());
        goal.setSavedAmount(source.getSavedAmount());
        goal.setDeadline(source.getDeadline());
        goal.setStatus(source.getStatus());
        goal.setCreatedAt(source.getCreatedAt());
        goal.setUpdatedAt(source.getUpdatedAt());
        goal.setIsDeleted(source.getIsDeleted());
        goal.setDeletedAt(source.getDeletedAt());
        return goal;
    }
}
