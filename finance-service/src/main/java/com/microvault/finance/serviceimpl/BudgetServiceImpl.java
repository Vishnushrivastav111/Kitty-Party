package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.DuplicateResourceException;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.BudgetService;
import com.microvault.finance.service.FinancialPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BudgetServiceImpl implements BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetServiceImpl.class);

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final GoalRepository goalRepository;
    private final BudgetEffects budgetEffects;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             TransactionRepository transactionRepository,
                             FinanceProfileRepository financeProfileRepository,
                             GoalRepository goalRepository,
                             BudgetEffects budgetEffects) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.financeProfileRepository = financeProfileRepository;
        this.goalRepository = goalRepository;
        this.budgetEffects = budgetEffects;
    }

    @Override
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        String category = request.getCategory().trim();
        Budget existing = budgetRepository
                .findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, category)
                .orElse(null);
        if (existing != null) {
            throw new DuplicateResourceException("A budget already exists for " + request.getCategory());
        }
        enforceCapacity(userId, null, request.getLimit());
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUserId(userId);
        copy(request, budget);
        LocalDateTime now = LocalDateTime.now();
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        budget.setDeleted(false);
        Budget saved = budgetRepository.save(budget);
        refreshAlerts(userId);
        return toResponse(saved, spentFor(userId, saved.getCategory()));
    }

    @Override
    public BudgetResponse update(UUID userId, UUID id, BudgetRequest request) {
        Budget budget = load(userId, id);
        Budget existing = budgetRepository
                .findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, request.getCategory().trim())
                .orElse(null);
        if (existing != null && !existing.getId().equals(budget.getId())) {
            throw new DuplicateResourceException("A budget already exists for " + request.getCategory());
        }
        enforceCapacity(userId, budget, request.getLimit());
        copy(request, budget);
        budget.setUpdatedAt(LocalDateTime.now());
        Budget saved = budgetRepository.save(budget);
        refreshAlerts(userId);
        return toResponse(saved, spentFor(userId, saved.getCategory()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId) {
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId);
        List<BudgetResponse> rows = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            rows.add(toResponse(budget, BudgetSpending.spent(transactions, budget.getCategory(), LocalDate.now())));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        Budget budget = load(userId, id);
        markDeleted(budget);
        budgetRepository.save(budget);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            markDeleted(budget);
            budgetRepository.save(budget);
        }
    }

    private void enforceCapacity(UUID userId, Budget editing, BigDecimal newLimit) {
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ValidationException("Complete financial setup before allocating a budget."));
        BigDecimal income = FinancialPlan.totalMonthlyIncome(profile.getMonthlyIncome(), profile.getAdditionalIncome());
        if (income.signum() <= 0) {
            throw new ValidationException("Add your monthly income in financial setup before allocating a budget.");
        }
        BigDecimal goalPlans = BigDecimal.ZERO;
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            if ("active".equalsIgnoreCase(goal.getStatus()) && goal.getPlannedContribution() != null) {
                goalPlans = goalPlans.add(goal.getPlannedContribution());
            }
        }
        BigDecimal capacity = FinancialPlan.budgetCapacity(income, goalPlans);
        BigDecimal allocated = BigDecimal.ZERO;
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            if (editing != null && editing.getId().equals(budget.getId())) {
                continue;
            }
            if (budget.getLimit() != null) {
                allocated = allocated.add(budget.getLimit());
            }
        }
        BigDecimal remaining = FinancialPlan.remainingBudgetCapacity(capacity, allocated);
        if (FinancialPlan.money(newLimit).compareTo(remaining) > 0) {
            throw new ValidationException(FinancialPlan.budgetCapacityMessage(capacity, allocated, remaining));
        }
    }

    private void refreshAlerts(UUID userId) {
        try {
            budgetEffects.refreshAfterCommit(userId);
        } catch (RuntimeException exception) {
            log.warn("Budget alerts were not refreshed: {}", exception.getMessage());
        }
    }

    private BigDecimal spentFor(UUID userId, String category) {
        return BudgetSpending.spent(
                transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId),
                category,
                LocalDate.now());
    }

    private void copy(BudgetRequest request, Budget budget) {
        budget.setCategory(request.getCategory().trim());
        budget.setLimit(request.getLimit());
        budget.setNote(request.getNote());
    }

    private Budget load(UUID userId, UUID id) {
        return budgetRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private void markDeleted(Budget budget) {
        LocalDateTime now = LocalDateTime.now();
        budget.setDeleted(true);
        budget.setDeletedAt(now);
        budget.setUpdatedAt(now);
    }

    private BudgetResponse toResponse(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getLimit() == null ? BigDecimal.ZERO : budget.getLimit();
        BigDecimal used = BigDecimal.ZERO;
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            used = spent.multiply(new BigDecimal("100")).divide(limit, 2, RoundingMode.HALF_UP);
        }
        BigDecimal remaining = limit.subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        String usage = "Within budget";
        if (used.compareTo(new BigDecimal("100")) > 0) {
            usage = "Over budget";
        } else if (used.compareTo(new BigDecimal("80")) >= 0) {
            usage = "Close to limit";
        }

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategory(budget.getCategory());
        response.setLimit(limit);
        response.setNote(budget.getNote());
        response.setSpent(spent);
        response.setRemaining(remaining);
        response.setUsedPercent(used);
        response.setUsage(usage);
        return response;
    }
}
