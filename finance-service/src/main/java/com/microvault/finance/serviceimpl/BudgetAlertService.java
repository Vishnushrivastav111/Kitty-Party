package com.microvault.finance.serviceimpl;

import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.BudgetNotification;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.repository.BudgetNotificationRepository;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.FinancialPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetAlertService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertService.class);

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetNotificationRepository notificationRepository;

    public BudgetAlertService(BudgetRepository budgetRepository,
                              TransactionRepository transactionRepository,
                              BudgetNotificationRepository notificationRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluate(UUID userId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId);
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId);
        YearMonth period = YearMonth.now();
        LocalDateTime expires = period.atEndOfMonth().atTime(23, 59, 59);
        LocalDate today = LocalDate.now();
        for (Budget budget : budgets) {
            BigDecimal spent = BudgetSpending.spent(transactions, budget.getCategory(), today);
            BigDecimal limit = budget.getLimit() == null ? BigDecimal.ZERO : budget.getLimit();
            if (limit.signum() <= 0) {
                continue;
            }
            BigDecimal percent = spent.multiply(new BigDecimal("100")).divide(limit, 2, RoundingMode.HALF_UP);
            for (int threshold : FinancialPlan.BUDGET_THRESHOLDS) {
                if (percent.compareTo(BigDecimal.valueOf(threshold)) < 0) {
                    continue;
                }
                String key = "budget:" + budget.getId() + ":" + period + ":" + threshold;
                if (notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(userId, key)) {
                    continue;
                }
                save(userId, budget, threshold, spent, limit, key, expires);
            }
        }
    }

    private void save(UUID userId, Budget budget, int threshold, BigDecimal spent, BigDecimal limit,
                      String key, LocalDateTime expires) {
        BudgetNotification notification = new BudgetNotification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setBudgetId(budget.getId());
        notification.setReferenceKey(key);
        notification.setTitle(title(threshold, budget.getCategory()));
        notification.setMessage(budget.getCategory() + " spending is " + FinancialPlan.rupee(spent)
                + " of a " + FinancialPlan.rupee(limit) + " monthly limit (" + threshold + "% threshold).");
        notification.setType(type(threshold));
        notification.setRead(false);
        notification.setDismissed(false);
        notification.setExpiresAt(expires);
        LocalDateTime now = LocalDateTime.now();
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setDeleted(false);
        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Budget alert already stored for {}", key);
        }
    }

    private String title(int threshold, String category) {
        if (threshold >= 100) {
            return category + " budget exhausted";
        }
        if (threshold >= 80) {
            return category + " budget warning";
        }
        if (threshold >= 50) {
            return category + " budget halfway";
        }
        return category + " budget update";
    }

    private String type(int threshold) {
        if (threshold >= 100) {
            return "danger";
        }
        if (threshold >= 80) {
            return "warning";
        }
        return "info";
    }
}
