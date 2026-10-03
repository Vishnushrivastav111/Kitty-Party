package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.BudgetRequest;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.exception.DuplicateResourceException;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.BudgetService;
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
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository, TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        Budget existing = budgetRepository
                .findByUserIdAndCategoryIgnoreCaseAndDeletedFalse(userId, request.getCategory().trim())
                .orElse(null);
        if (existing != null) {
            throw new DuplicateResourceException("A budget already exists for " + request.getCategory());
        }
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUserId(userId);
        copy(request, budget);
        LocalDateTime now = LocalDateTime.now();
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        budget.setDeleted(false);
        return toResponse(budgetRepository.save(budget), spentFor(userId, budget.getCategory()));
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
        copy(request, budget);
        budget.setUpdatedAt(LocalDateTime.now());
        return toResponse(budgetRepository.save(budget), spentFor(userId, budget.getCategory()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId) {
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId);
        List<BudgetResponse> rows = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            rows.add(toResponse(budget, spentIn(transactions, budget.getCategory())));
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

    private BigDecimal spentFor(UUID userId, String category) {
        return spentIn(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId), category);
    }

    private BigDecimal spentIn(List<Transaction> transactions, String category) {
        BigDecimal spent = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            boolean expense = "expense".equalsIgnoreCase(transaction.getType());
            boolean sameCategory = category != null && category.equalsIgnoreCase(transaction.getCategory());
            if (expense && sameCategory && transaction.getAmount() != null) {
                spent = spent.add(transaction.getAmount());
            }
        }
        return spent;
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
