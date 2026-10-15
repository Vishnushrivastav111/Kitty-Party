package com.microvault.finance.serviceimpl;

import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.BudgetNotification;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.entity.Transaction;
import com.microvault.finance.repository.BudgetNotificationRepository;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import com.microvault.finance.repository.TransactionRepository;
import com.microvault.finance.service.FinancialPlan;
import com.microvault.finance.service.SavingsPosition;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Covers budget overruns from savings.
 *
 * <p>When a category's spending for the month goes past 100% of its limit, the amount over the limit is taken
 * out of savings in a fixed order:
 * <ol>
 *   <li>unallocated liquid savings,</li>
 *   <li>goal savings (newest goal first),</li>
 *   <li>the emergency reserve, only as a last resort.</li>
 * </ol>
 * Every deduction is a savings entry tagged with the budget and month, so the work is idempotent: each run
 * compares the current overrun with what has already been covered and only moves the difference. If a
 * transaction is edited or deleted and the overrun shrinks, the most recent deductions are given back first,
 * which reverses the order above.
 */
@Service
public class BudgetOverrunCoverage {

    static final String TAG_PREFIX = "auto-overrun:";

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final SavingsRepository savingsRepository;
    private final GoalRepository goalRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final BudgetNotificationRepository notificationRepository;

    public BudgetOverrunCoverage(BudgetRepository budgetRepository,
                                 TransactionRepository transactionRepository,
                                 SavingsRepository savingsRepository,
                                 GoalRepository goalRepository,
                                 FinanceProfileRepository financeProfileRepository,
                                 BudgetNotificationRepository notificationRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.savingsRepository = savingsRepository;
        this.goalRepository = goalRepository;
        this.financeProfileRepository = financeProfileRepository;
        this.notificationRepository = notificationRepository;
    }

    /** What one run moved for one budget, mainly for messages and tests. */
    public record Coverage(BigDecimal fromUnallocated, BigDecimal fromGoals, BigDecimal fromReserve) {
        BigDecimal total() {
            return fromUnallocated.add(fromGoals).add(fromReserve);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcile(UUID userId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId);
        if (budgets.isEmpty()) {
            return;
        }
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId);
        List<SavingsEntry> entries = new ArrayList<>(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId));
        List<Goal> goals = new ArrayList<>(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId));
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        YearMonth period = YearMonth.now();
        LocalDate today = LocalDate.now();

        for (Budget budget : budgets) {
            BigDecimal limit = budget.getLimit() == null ? BigDecimal.ZERO : budget.getLimit();
            if (limit.signum() <= 0) {
                continue;
            }
            BigDecimal spent = BudgetSpending.spent(transactions, budget.getCategory(), today);
            BigDecimal overrun = FinancialPlan.nonNegative(spent.subtract(limit));
            String tag = TAG_PREFIX + budget.getId() + ":" + period;
            List<SavingsEntry> tagged = new ArrayList<>();
            for (SavingsEntry entry : entries) {
                if (tag.equals(entry.getNote())) {
                    tagged.add(entry);
                }
            }
            BigDecimal covered = BigDecimal.ZERO;
            for (SavingsEntry entry : tagged) {
                covered = covered.add(entry.getAmount().abs());
            }
            int comparison = overrun.compareTo(FinancialPlan.money(covered));
            if (comparison > 0) {
                Coverage moved = cover(userId, budget, tag, overrun.subtract(covered), profile, entries, goals);
                notifyCovered(userId, budget, period, limit, overrun, covered.add(moved.total()), moved);
            } else if (comparison < 0) {
                giveBack(userId, tagged, covered.subtract(overrun), goals, entries);
            }
        }
    }

    /** Takes {@code amount} from savings in priority order and returns what came from where. */
    Coverage cover(UUID userId, Budget budget, String tag, BigDecimal amount, FinanceProfile profile,
                   List<SavingsEntry> entries, List<Goal> goals) {
        BigDecimal remaining = FinancialPlan.money(amount);
        SavingsPosition.Snapshot position = SavingsPosition.of(profile, entries, goals);
        String title = "Budget overrun: " + budget.getCategory();

        BigDecimal fromUnallocated = take(remaining, position.unallocatedLiquid());
        if (fromUnallocated.signum() > 0) {
            entries.add(savings(userId, title, FinancialPlan.WITHDRAWAL_CATEGORY, fromUnallocated, null, tag));
            remaining = remaining.subtract(fromUnallocated);
        }

        BigDecimal fromGoals = BigDecimal.ZERO;
        for (Goal goal : goals) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (goal.getSaved() == null || "archived".equalsIgnoreCase(goal.getStatus())) {
                continue;
            }
            BigDecimal taken = take(remaining, FinancialPlan.nonNegative(goal.getSaved()));
            if (taken.signum() <= 0) {
                continue;
            }
            entries.add(savings(userId, title + " (" + goal.getTitle() + ")",
                    FinancialPlan.WITHDRAWAL_CATEGORY, taken, goal.getId(), tag));
            changeGoal(goal, taken.negate());
            fromGoals = fromGoals.add(taken);
            remaining = remaining.subtract(taken);
        }

        BigDecimal fromReserve = take(remaining, position.emergencyReserve());
        if (fromReserve.signum() > 0) {
            entries.add(savings(userId, title, FinancialPlan.EMERGENCY_WITHDRAWAL_CATEGORY, fromReserve, null, tag));
        }
        return new Coverage(fromUnallocated, fromGoals, fromReserve);
    }

    /** Returns savings after an overrun shrinks. The most recent deduction is returned first. */
    private void giveBack(UUID userId, List<SavingsEntry> tagged, BigDecimal excess, List<Goal> goals,
                          List<SavingsEntry> entries) {
        List<SavingsEntry> newestFirst = new ArrayList<>(tagged);
        Comparator<SavingsEntry> oldestFirst = Comparator.comparing(SavingsEntry::getCreatedAt,
                Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()));
        newestFirst.sort(oldestFirst.reversed());
        BigDecimal toReturn = FinancialPlan.money(excess);
        for (SavingsEntry entry : newestFirst) {
            if (toReturn.signum() <= 0) {
                break;
            }
            BigDecimal amount = entry.getAmount().abs();
            BigDecimal returned = amount.min(toReturn);
            if (returned.compareTo(amount) >= 0) {
                entry.setDeleted(true);
                entry.setDeletedAt(LocalDateTime.now());
                entries.remove(entry);
            } else {
                entry.setAmount(amount.subtract(returned));
            }
            entry.setUpdatedAt(LocalDateTime.now());
            savingsRepository.save(entry);
            if (entry.getGoalId() != null) {
                for (Goal goal : goals) {
                    if (entry.getGoalId().equals(goal.getId())) {
                        changeGoal(goal, returned);
                    }
                }
            }
            toReturn = toReturn.subtract(returned);
        }
    }

    private void changeGoal(Goal goal, BigDecimal delta) {
        BigDecimal current = goal.getSaved() == null ? BigDecimal.ZERO : goal.getSaved();
        BigDecimal next = FinancialPlan.nonNegative(current.add(delta));
        if (goal.getTarget() != null && next.compareTo(goal.getTarget()) > 0) {
            next = goal.getTarget();
        }
        goal.setSaved(next);
        if ("completed".equalsIgnoreCase(goal.getStatus()) && goal.getTarget() != null
                && next.compareTo(goal.getTarget()) < 0) {
            goal.setStatus("active");
        }
        goal.setUpdatedAt(LocalDateTime.now());
        goalRepository.save(goal);
    }

    private SavingsEntry savings(UUID userId, String title, String category, BigDecimal amount, UUID goalId, String tag) {
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        entry.setTitle(title.length() > 120 ? title.substring(0, 120) : title);
        entry.setCategory(category);
        entry.setAmount(FinancialPlan.money(amount));
        entry.setDate(LocalDate.now());
        entry.setNote(tag);
        entry.setGoalId(goalId);
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setDeleted(false);
        savingsRepository.save(entry);
        return entry;
    }

    private static BigDecimal take(BigDecimal needed, BigDecimal available) {
        if (needed.signum() <= 0 || available == null || available.signum() <= 0) {
            return FinancialPlan.money(null);
        }
        return FinancialPlan.money(needed.min(available));
    }

    private void notifyCovered(UUID userId, Budget budget, YearMonth period, BigDecimal limit, BigDecimal overrun,
                               BigDecimal totalCovered, Coverage moved) {
        if (moved.total().signum() <= 0) {
            return;
        }
        String key = "overrun:" + budget.getId() + ":" + period + ":" + FinancialPlan.money(totalCovered).toPlainString();
        if (notificationRepository.existsByUserIdAndReferenceKeyAndDeletedFalse(userId, key)) {
            return;
        }
        StringBuilder message = new StringBuilder(budget.getCategory()).append(" is ")
                .append(FinancialPlan.rupee(overrun)).append(" over its ").append(FinancialPlan.rupee(limit))
                .append(" limit. ").append(FinancialPlan.rupee(moved.total())).append(" was taken from your savings");
        List<String> parts = new ArrayList<>();
        if (moved.fromUnallocated().signum() > 0) {
            parts.add(FinancialPlan.rupee(moved.fromUnallocated()) + " from available savings");
        }
        if (moved.fromGoals().signum() > 0) {
            parts.add(FinancialPlan.rupee(moved.fromGoals()) + " from goal savings");
        }
        if (moved.fromReserve().signum() > 0) {
            parts.add(FinancialPlan.rupee(moved.fromReserve()) + " from your emergency reserve");
        }
        message.append(" (").append(String.join(", ", parts)).append(").");
        BigDecimal uncovered = overrun.subtract(totalCovered);
        if (uncovered.signum() > 0) {
            message.append(" ").append(FinancialPlan.rupee(uncovered)).append(" could not be covered.");
        }
        LocalDateTime now = LocalDateTime.now();
        BudgetNotification notification = new BudgetNotification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setBudgetId(budget.getId());
        notification.setReferenceKey(key);
        notification.setTitle(budget.getCategory() + " overrun covered from savings");
        notification.setMessage(message.toString());
        notification.setType("danger");
        notification.setRead(false);
        notification.setDismissed(false);
        notification.setExpiresAt(period.atEndOfMonth().atTime(23, 59, 59));
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setDeleted(false);
        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException duplicate) {
            // Already stored for this amount.
        }
    }
}
