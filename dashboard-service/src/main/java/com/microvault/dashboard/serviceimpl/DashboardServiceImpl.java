package com.microvault.dashboard.serviceimpl;

import com.microvault.dashboard.exception.UserNotFoundException;
import com.microvault.dashboard.exception.ValidationException;
import com.microvault.dashboard.model.BudgetItem;
import com.microvault.dashboard.model.DashboardData;
import com.microvault.dashboard.model.DashboardStats;
import com.microvault.dashboard.model.FinanceSnapshot;
import com.microvault.dashboard.model.GoalItem;
import com.microvault.dashboard.model.Member;
import com.microvault.dashboard.model.SavingItem;
import com.microvault.dashboard.model.TransactionItem;
import com.microvault.dashboard.repository.BudgetItemRepository;
import com.microvault.dashboard.repository.FinanceSnapshotRepository;
import com.microvault.dashboard.repository.GoalItemRepository;
import com.microvault.dashboard.repository.MemberRepository;
import com.microvault.dashboard.repository.SavingItemRepository;
import com.microvault.dashboard.repository.TransactionItemRepository;
import com.microvault.dashboard.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final MemberRepository memberRepository;
    private final FinanceSnapshotRepository financeSnapshotRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final GoalItemRepository goalItemRepository;
    private final SavingItemRepository savingItemRepository;
    private final BudgetItemRepository budgetItemRepository;

    public DashboardServiceImpl(MemberRepository memberRepository,
                                FinanceSnapshotRepository financeSnapshotRepository,
                                TransactionItemRepository transactionItemRepository,
                                GoalItemRepository goalItemRepository,
                                SavingItemRepository savingItemRepository,
                                BudgetItemRepository budgetItemRepository) {
        this.memberRepository = memberRepository;
        this.financeSnapshotRepository = financeSnapshotRepository;
        this.transactionItemRepository = transactionItemRepository;
        this.goalItemRepository = goalItemRepository;
        this.savingItemRepository = savingItemRepository;
        this.budgetItemRepository = budgetItemRepository;
    }

    @Override
    public DashboardData load(UUID userId, String email) {
        String cleanEmail = clean(email);
        if (userId == null && cleanEmail == null) {
            throw new ValidationException("Email or user id is required");
        }

        Member member = findMember(userId, cleanEmail);
        FinanceSnapshot finance = financeSnapshotRepository.findByUserIdAndDeletedFalse(member.getId()).orElse(null);
        List<TransactionItem> transactions =
                transactionItemRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(member.getId());
        List<GoalItem> goals = goalItemRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(member.getId());
        List<SavingItem> savings = savingItemRepository.findByUserIdAndDeletedFalseOrderByDateDesc(member.getId());
        List<BudgetItem> budgets = budgetItemRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(member.getId());

        DashboardData data = new DashboardData();
        data.setUser(member);
        data.setFinance(finance);
        data.setTransactions(transactions);
        data.setGoals(goals);
        data.setSavings(savings);
        data.setBudgets(budgets);
        data.setStats(buildStats(member, finance, transactions, goals, savings, budgets));
        return data;
    }

    private Member findMember(UUID userId, String email) {
        if (userId != null) {
            Optional<Member> byId = memberRepository.findByIdAndDeletedFalse(userId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (email != null) {
            Optional<Member> byEmail = memberRepository.findByEmailIgnoreCaseAndDeletedFalse(email);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        throw new UserNotFoundException("No user found for this dashboard");
    }

    private String clean(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    /**
     * Card numbers: total saved, money spent, the monthly budget,
     * and a health score from how much income is left after expenses.
     */
    private DashboardStats buildStats(Member member,
                                      FinanceSnapshot finance,
                                      List<TransactionItem> transactions,
                                      List<GoalItem> goals,
                                      List<SavingItem> savings,
                                      List<BudgetItem> budgets) {

        BigDecimal totalSavings = BigDecimal.ZERO;
        for (SavingItem row : savings) {
            totalSavings = totalSavings.add(money(row.getAmount()));
        }

        BigDecimal spent = BigDecimal.ZERO;
        for (TransactionItem transaction : transactions) {
            if (transaction.getType() != null && transaction.getType().equalsIgnoreCase("expense")) {
                spent = spent.add(money(transaction.getAmount()));
            }
        }

        BigDecimal income = finance == null ? BigDecimal.ZERO : money(finance.getMonthlyIncome());
        BigDecimal profileExpenses = finance == null ? null : finance.getMonthlyExpenses();
        BigDecimal expenses = (profileExpenses == null || profileExpenses.compareTo(BigDecimal.ZERO) == 0)
                ? spent
                : profileExpenses;

        BigDecimal monthBudget = finance == null ? null : finance.getMonthlyBudget();
        if (monthBudget == null || monthBudget.compareTo(BigDecimal.ZERO) == 0) {
            monthBudget = BigDecimal.ZERO;
            for (BudgetItem budget : budgets) {
                monthBudget = monthBudget.add(money(budget.getLimit()));
            }
            if (monthBudget.compareTo(BigDecimal.ZERO) == 0) {
                monthBudget = new BigDecimal("30000");
            }
        }

        int health = 50;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal left = income.subtract(expenses);
            if (left.compareTo(BigDecimal.ZERO) < 0) {
                left = BigDecimal.ZERO;
            }
            BigDecimal saveRate = left.divide(income, 4, RoundingMode.HALF_UP);
            double score = 40 + saveRate.doubleValue() * 60;
            if (totalSavings.compareTo(BigDecimal.ZERO) > 0) {
                score = score + 10;
            }
            if (score > 100) {
                score = 100;
            }
            health = (int) Math.round(score);
        }

        int activeGoals = 0;
        for (GoalItem goal : goals) {
            if (goal.getStatus() == null || !goal.getStatus().equalsIgnoreCase("completed")) {
                activeGoals = activeGoals + 1;
            }
        }

        DashboardStats stats = new DashboardStats();
        stats.setFullName(member.getFullName());
        stats.setHealth(health);
        stats.setTotalSavings(totalSavings);
        stats.setMonthBudget(monthBudget);
        stats.setSpent(spent);
        stats.setActiveGoals(activeGoals);
        stats.setIncome(income);
        stats.setExpenses(expenses);
        return stats;
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount;
    }
}
