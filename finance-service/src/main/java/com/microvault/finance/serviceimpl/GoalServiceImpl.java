package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.service.GoalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;

    public GoalServiceImpl(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Override
    public GoalResponse create(UUID userId, GoalRequest request) {
        validate(request);
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        copy(request, goal);
        LocalDateTime now = LocalDateTime.now();
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        goal.setDeleted(false);
        return toResponse(goalRepository.save(goal));
    }

    @Override
    public GoalResponse update(UUID userId, UUID id, GoalRequest request) {
        validate(request);
        Goal goal = load(userId, id);
        copy(request, goal);
        goal.setUpdatedAt(LocalDateTime.now());
        return toResponse(goalRepository.save(goal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId) {
        List<GoalResponse> rows = new ArrayList<>();
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            rows.add(toResponse(goal));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        Goal goal = load(userId, id);
        LocalDateTime now = LocalDateTime.now();
        goal.setDeleted(true);
        goal.setDeletedAt(now);
        goal.setUpdatedAt(now);
        goalRepository.save(goal);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            goal.setDeleted(true);
            goal.setDeletedAt(now);
            goal.setUpdatedAt(now);
            goalRepository.save(goal);
        }
    }

    private void validate(GoalRequest request) {
        BigDecimal saved = request.getSaved() == null ? BigDecimal.ZERO : request.getSaved();
        if (saved.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Saved amount cannot be negative");
        }
        if (saved.compareTo(request.getTarget()) > 0) {
            throw new ValidationException("Saved cannot exceed target");
        }
    }

    private void copy(GoalRequest request, Goal goal) {
        BigDecimal saved = request.getSaved() == null ? BigDecimal.ZERO : request.getSaved();
        goal.setTitle(request.getTitle().trim());
        goal.setCategory(request.getCategory());
        goal.setTarget(request.getTarget());
        goal.setSaved(saved);
        goal.setDeadline(request.getDeadline());
        if (saved.compareTo(request.getTarget()) >= 0) {
            goal.setStatus("completed");
            return;
        }
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toLowerCase();
        if (!"active".equals(status) && !"paused".equals(status)) {
            status = "active";
        }
        goal.setStatus(status);
    }

    private Goal load(UUID userId, UUID id) {
        return goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal saved = goal.getSaved() == null ? BigDecimal.ZERO : goal.getSaved();
        BigDecimal target = goal.getTarget() == null ? BigDecimal.ZERO : goal.getTarget();
        BigDecimal progress = BigDecimal.ZERO;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            progress = saved.multiply(new BigDecimal("100")).divide(target, 2, RoundingMode.HALF_UP);
            if (progress.compareTo(new BigDecimal("100")) > 0) {
                progress = new BigDecimal("100.00");
            }
        }
        BigDecimal remaining = target.subtract(saved);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setTitle(goal.getTitle());
        response.setCategory(goal.getCategory());
        response.setTarget(target);
        response.setSaved(saved);
        response.setDeadline(goal.getDeadline());
        response.setStatus(goal.getStatus());
        response.setProgressPercent(progress);
        response.setRemaining(remaining);
        return response;
    }
}
