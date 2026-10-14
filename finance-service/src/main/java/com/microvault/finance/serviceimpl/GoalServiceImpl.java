package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import com.microvault.finance.service.FinancialPlan;
import com.microvault.finance.service.GoalProgress;
import com.microvault.finance.service.GoalService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final SavingsRepository savingsRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final BudgetRepository budgetRepository;
    private final EntityManager entityManager;

    public GoalServiceImpl(GoalRepository goalRepository,
                           SavingsRepository savingsRepository,
                           FinanceProfileRepository financeProfileRepository,
                           BudgetRepository budgetRepository,
                           EntityManager entityManager) {
        this.goalRepository = goalRepository;
        this.savingsRepository = savingsRepository;
        this.financeProfileRepository = financeProfileRepository;
        this.budgetRepository = budgetRepository;
        this.entityManager = entityManager;
    }

    @Override
    public GoalResponse create(UUID userId, GoalRequest request) {
        lockUser(userId);
        validateAmounts(request);
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        apply(userId, goal, request, List.of());
        LocalDateTime now = LocalDateTime.now();
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        goal.setDeleted(false);
        return toResponse(goalRepository.save(goal), BigDecimal.ZERO, userId);
    }

    @Override
    public GoalResponse update(UUID userId, UUID id, GoalRequest request) {
        lockUser(userId);
        validateAmounts(request);
        Goal goal = load(userId, id);
        List<SavingsEntry> entries = savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        apply(userId, goal, request, entries);
        goal.setUpdatedAt(LocalDateTime.now());
        BigDecimal contributed = GoalProgress.contributed(entries, goal.getId());
        return toResponse(goalRepository.save(goal), contributed, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId) {
        List<SavingsEntry> entries = savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        List<GoalResponse> rows = new ArrayList<>();
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            rows.add(toResponse(goal, GoalProgress.contributed(entries, goal.getId()), userId));
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

    private void apply(UUID userId, Goal goal, GoalRequest request, List<SavingsEntry> entries) {
        BigDecimal target = request.getTarget();
        BigDecimal contributed = GoalProgress.contributed(entries, goal.getId());
        BigDecimal opening = request.getSaved() == null ? GoalProgress.opening(goal) : request.getSaved();
        if (Boolean.TRUE.equals(request.getAllocateExisting())) {
            BigDecimal delta = opening.subtract(GoalProgress.opening(goal));
            moveExistingSavings(userId, delta);
        }
        BigDecimal saved = opening.add(contributed);
        if (saved.compareTo(target) > 0) {
            throw new ValidationException("Saved cannot exceed target");
        }
        String status = normalizeStatus(request.getStatus(), saved, target);
        if ("active".equals(status)) {
            enforceActiveLimit(userId, goal.getId());
            assertFeasible(userId, goal.getId(), target, saved, request.getDeadline(), request.getPlannedContribution(), goal);
        }
        goal.setTitle(request.getTitle().trim());
        goal.setCategory(clean(request.getCategory()));
        goal.setDescription(clean(request.getDescription()));
        goal.setPriority(priority(request.getPriority()));
        goal.setTarget(target);
        goal.setOpeningSaved(opening);
        goal.setSaved(saved);
        goal.setDeadline(request.getDeadline());
        goal.setStatus(status);
        if (!"active".equals(status)) {
            goal.setPlannedContribution(FinancialPlan.money(request.getPlannedContribution()));
        }
    }

    private void assertFeasible(UUID userId, UUID goalId, BigDecimal target, BigDecimal saved,
                                LocalDate deadline, BigDecimal plannedInput, Goal goal) {
        if (deadline == null) {
            throw new ValidationException("Choose a deadline before activating this goal, or save it as a draft.");
        }
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        if (profile == null) {
            throw new ValidationException("Complete financial setup before activating a goal.");
        }
        int periods = FinancialPlan.contributionPeriods(LocalDate.now(), deadline);
        BigDecimal remaining = target.subtract(saved);
        if (periods == 0 && remaining.signum() > 0) {
            throw new ValidationException("The deadline has passed. Extend the deadline, reduce the target, or save the goal as a draft.");
        }
        BigDecimal required = FinancialPlan.requiredMonthly(remaining, periods);
        BigDecimal capacity = goalCapacity(userId, profile, goalId);
        BigDecimal planned = plannedInput == null ? required : FinancialPlan.money(plannedInput);
        if (required != null && planned.compareTo(required) < 0) {
            throw new ValidationException("The planned contribution is below the " + FinancialPlan.rupee(required)
                    + " required each month. Extend the deadline, reduce the target, or raise the contribution.");
        }
        BigDecimal room = FinancialPlan.nonNegative(capacity);
        if (planned != null && planned.compareTo(room) > 0) {
            BigDecimal shortfall = planned.subtract(room);
            throw new ValidationException("This goal needs " + FinancialPlan.rupee(planned)
                    + " per month, but only " + FinancialPlan.rupee(room)
                    + " is available after obligations, budgets, and other active goals. Monthly shortfall is "
                    + FinancialPlan.rupee(shortfall)
                    + ". Extend the deadline, reduce the target, pause another goal, or save this goal as a draft.");
        }
        goal.setPlannedContribution(planned == null ? FinancialPlan.money(null) : planned);
    }

    private BigDecimal goalCapacity(UUID userId, FinanceProfile profile, UUID ignoreGoalId) {
        BigDecimal income = FinancialPlan.totalMonthlyIncome(profile.getMonthlyIncome(), profile.getAdditionalIncome());
        BigDecimal planning = FinancialPlan.planningCapacity(income, profile.getMonthlyExpenses(),
                profile.getMonthlyEmi(), profile.getOtherCommitments());
        BigDecimal budgets = BigDecimal.ZERO;
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            if (budget.getLimit() != null) {
                budgets = budgets.add(budget.getLimit());
            }
        }
        BigDecimal otherGoals = BigDecimal.ZERO;
        for (Goal existing : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            if (ignoreGoalId != null && ignoreGoalId.equals(existing.getId())) {
                continue;
            }
            if ("active".equalsIgnoreCase(existing.getStatus()) && existing.getPlannedContribution() != null) {
                otherGoals = otherGoals.add(existing.getPlannedContribution());
            }
        }
        return FinancialPlan.goalContributionCapacity(planning, budgets, otherGoals);
    }

    private void enforceActiveLimit(UUID userId, UUID ignoreId) {
        int active = 0;
        for (Goal existing : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            if (ignoreId != null && ignoreId.equals(existing.getId())) {
                continue;
            }
            if ("active".equalsIgnoreCase(existing.getStatus())) {
                active++;
            }
        }
        if (active >= FinancialPlan.MAX_ACTIVE_GOALS) {
            throw new ValidationException("You can have at most " + FinancialPlan.MAX_ACTIVE_GOALS
                    + " active goals. Pause, complete, or archive one before activating another. Existing goals are kept.");
        }
    }

    private BigDecimal moveExistingSavings(UUID userId, BigDecimal amount) {
        BigDecimal move = FinancialPlan.money(amount);
        if (move.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ValidationException("Complete financial setup before allocating existing savings."));
        BigDecimal liquid = FinancialPlan.money(profile.getCurrentSavings());
        BigDecimal emergency = FinancialPlan.money(profile.getEmergencyReserve());
        if (emergency.compareTo(liquid) > 0) {
            emergency = liquid;
        }
        BigDecimal unallocated = liquid.subtract(emergency);
        if (move.compareTo(unallocated) > 0) {
            throw new ValidationException("Only " + FinancialPlan.rupee(unallocated)
                    + " of unallocated savings can be assigned. Emergency reserves stay protected.");
        }
        profile.setCurrentSavings(liquid.subtract(move));
        profile.setUpdatedAt(LocalDateTime.now());
        financeProfileRepository.save(profile);
        return move;
    }

    private void validateAmounts(GoalRequest request) {
        if (request.getTarget() == null || request.getTarget().signum() <= 0) {
            throw new ValidationException("Target must be greater than zero");
        }
        BigDecimal saved = request.getSaved() == null ? BigDecimal.ZERO : request.getSaved();
        if (saved.signum() < 0) {
            throw new ValidationException("Saved amount cannot be negative");
        }
        if (saved.compareTo(request.getTarget()) > 0) {
            throw new ValidationException("Saved cannot exceed target");
        }
        if (request.getPlannedContribution() != null && request.getPlannedContribution().signum() < 0) {
            throw new ValidationException("Planned contribution cannot be negative");
        }
    }

    private String normalizeStatus(String raw, BigDecimal saved, BigDecimal target) {
        if (saved.compareTo(target) >= 0) {
            return "completed";
        }
        String status = raw == null ? "" : raw.trim().toLowerCase();
        return switch (status) {
            case "draft", "active", "paused", "completed", "archived" -> status;
            case "" -> "active";
            default -> "draft";
        };
    }

    private String priority(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return switch (value) {
            case "low", "high" -> value;
            default -> "medium";
        };
    }

    private void lockUser(UUID userId) {
        if (entityManager == null) {
            return;
        }
        Query query = entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?, ?)");
        long msb = userId.getMostSignificantBits();
        long lsb = userId.getLeastSignificantBits();
        query.setParameter(1, (int) (msb ^ (msb >>> 32)));
        query.setParameter(2, (int) (lsb ^ (lsb >>> 32)));
        query.getSingleResult();
    }

    private Goal load(UUID userId, UUID id) {
        return goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal goal, BigDecimal contributed, UUID userId) {
        BigDecimal opening = goal.getOpeningSaved() == null ? BigDecimal.ZERO : goal.getOpeningSaved();
        BigDecimal saved = opening.signum() > 0 || contributed.signum() > 0
                ? opening.add(contributed)
                : (goal.getSaved() == null ? BigDecimal.ZERO : goal.getSaved());
        BigDecimal target = goal.getTarget() == null ? BigDecimal.ZERO : goal.getTarget();
        BigDecimal progress = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (target.signum() > 0) {
            progress = saved.multiply(new BigDecimal("100")).divide(target, 2, RoundingMode.HALF_UP);
            if (progress.compareTo(new BigDecimal("100")) > 0) {
                progress = new BigDecimal("100.00");
            }
        }
        BigDecimal remaining = target.subtract(saved);
        if (remaining.signum() < 0) {
            remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int periods = FinancialPlan.contributionPeriods(LocalDate.now(), goal.getDeadline());
        BigDecimal required = FinancialPlan.requiredMonthly(remaining, periods);
        boolean feasible = !"active".equalsIgnoreCase(goal.getStatus()) || required == null
                || required.compareTo(FinancialPlan.nonNegative(capacityOrZero(userId, goal.getId()))) <= 0;

        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setTitle(goal.getTitle());
        response.setCategory(goal.getCategory());
        response.setDescription(goal.getDescription());
        response.setPriority(goal.getPriority());
        response.setTarget(target);
        response.setSaved(saved);
        response.setDeadline(goal.getDeadline());
        response.setStatus(goal.getStatus());
        response.setPlannedContribution(FinancialPlan.money(goal.getPlannedContribution()));
        response.setProgressPercent(progress);
        response.setRemaining(remaining);
        response.setRequiredMonthly(required);
        response.setContributionPeriods(goal.getDeadline() == null ? null : periods);
        response.setFeasible(feasible);
        response.setFeasibilityMessage(feasible
                ? "The planned contribution fits the monthly capacity that remains after obligations and budgets."
                : "The required contribution is above the monthly capacity that remains after obligations and budgets.");
        return response;
    }

    private BigDecimal capacityOrZero(UUID userId, UUID goalId) {
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        if (profile == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return goalCapacity(userId, profile, goalId);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
