package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import com.microvault.finance.service.FinancialPlan;
import com.microvault.finance.service.GoalProgress;
import com.microvault.finance.service.SavingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SavingsServiceImpl implements SavingsService {

    private final SavingsRepository savingsRepository;
    private final GoalRepository goalRepository;

    public SavingsServiceImpl(SavingsRepository savingsRepository, GoalRepository goalRepository) {
        this.savingsRepository = savingsRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    public SavingsResponse create(UUID userId, SavingsRequest request) {
        validate(request);
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        copy(userId, request, entry, null);
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setDeleted(false);
        SavingsEntry saved = savingsRepository.save(entry);
        refreshGoal(userId, saved.getGoalId());
        return toResponse(saved);
    }

    @Override
    public SavingsResponse update(UUID userId, UUID id, SavingsRequest request) {
        validate(request);
        SavingsEntry entry = load(userId, id);
        UUID previousGoal = entry.getGoalId();
        copy(userId, request, entry, entry.getId());
        entry.setUpdatedAt(LocalDateTime.now());
        SavingsEntry saved = savingsRepository.save(entry);
        refreshGoal(userId, previousGoal);
        if (saved.getGoalId() != null && !saved.getGoalId().equals(previousGoal)) {
            refreshGoal(userId, saved.getGoalId());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsResponse> list(UUID userId) {
        List<SavingsResponse> rows = new ArrayList<>();
        for (SavingsEntry entry : savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)) {
            rows.add(toResponse(entry));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        SavingsEntry entry = load(userId, id);
        markDeleted(entry);
        savingsRepository.save(entry);
        refreshGoal(userId, entry.getGoalId());
    }

    @Override
    public void softDeleteAll(UUID userId) {
        List<SavingsEntry> entries = savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        for (SavingsEntry entry : entries) {
            markDeleted(entry);
            savingsRepository.save(entry);
        }
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            refreshGoal(userId, goal.getId());
        }
    }

    private void copy(UUID userId, SavingsRequest request, SavingsEntry entry, UUID ignoreEntryId) {
        entry.setTitle(request.getTitle().trim());
        entry.setCategory(request.getCategory());
        entry.setAmount(request.getAmount());
        entry.setDate(request.getDate());
        entry.setNote(request.getNote());
        if (request.getGoalId() == null) {
            entry.setGoalId(null);
            return;
        }
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(request.getGoalId(), userId)
                .orElseThrow(() -> new ValidationException("Choose a goal that belongs to you."));
        if ("archived".equalsIgnoreCase(goal.getStatus())) {
            throw new ValidationException("Archived goals cannot receive new contributions.");
        }
        BigDecimal already = contributedExcept(userId, goal.getId(), ignoreEntryId);
        BigDecimal opening = GoalProgress.opening(goal);
        BigDecimal remaining = goal.getTarget().subtract(opening.add(already));
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new ValidationException("This contribution exceeds the remaining target of " + FinancialPlan.rupee(remaining) + ".");
        }
        entry.setGoalId(goal.getId());
    }

    private BigDecimal contributedExcept(UUID userId, UUID goalId, UUID ignoreEntryId) {
        BigDecimal total = BigDecimal.ZERO;
        for (SavingsEntry entry : savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)) {
            if (ignoreEntryId != null && ignoreEntryId.equals(entry.getId())) {
                continue;
            }
            if (goalId.equals(entry.getGoalId()) && entry.getAmount() != null) {
                total = total.add(entry.getAmount());
            }
        }
        return total;
    }

    private void refreshGoal(UUID userId, UUID goalId) {
        if (goalId == null) {
            return;
        }
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedFalse(goalId, userId).orElse(null);
        if (goal == null) {
            return;
        }
        BigDecimal contributed = GoalProgress.contributed(
                savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId), goalId);
        BigDecimal opening = goal.getOpeningSaved() == null ? BigDecimal.ZERO : goal.getOpeningSaved();
        BigDecimal total = opening.add(contributed);
        if (goal.getTarget() != null && total.compareTo(goal.getTarget()) > 0) {
            total = goal.getTarget();
        }
        goal.setSaved(total);
        if (goal.getTarget() != null && total.compareTo(goal.getTarget()) >= 0
                && !"archived".equalsIgnoreCase(goal.getStatus())) {
            goal.setStatus("completed");
            goal.setPlannedContribution(BigDecimal.ZERO);
        }
        goal.setUpdatedAt(LocalDateTime.now());
        goalRepository.save(goal);
    }

    private void validate(SavingsRequest request) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future");
        }
        if (FinancialPlan.emergencyCategory(request.getCategory()) && request.getGoalId() != null) {
            throw new ValidationException("Emergency reserves are not assigned to a goal.");
        }
    }

    private SavingsEntry load(UUID userId, UUID id) {
        return savingsRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings entry not found"));
    }

    private void markDeleted(SavingsEntry entry) {
        LocalDateTime now = LocalDateTime.now();
        entry.setDeleted(true);
        entry.setDeletedAt(now);
        entry.setUpdatedAt(now);
    }

    private SavingsResponse toResponse(SavingsEntry entry) {
        SavingsResponse response = new SavingsResponse();
        response.setId(entry.getId());
        response.setTitle(entry.getTitle());
        response.setCategory(entry.getCategory());
        response.setAmount(entry.getAmount());
        response.setDate(entry.getDate());
        response.setNote(entry.getNote());
        response.setGoalId(entry.getGoalId());
        return response;
    }
}
