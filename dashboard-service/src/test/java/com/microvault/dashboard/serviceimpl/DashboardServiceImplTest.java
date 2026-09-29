package com.microvault.dashboard.serviceimpl;

import com.microvault.dashboard.exception.UserNotFoundException;
import com.microvault.dashboard.exception.ValidationException;
import com.microvault.dashboard.model.BudgetItem;
import com.microvault.dashboard.model.DashboardData;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FinanceSnapshotRepository financeSnapshotRepository;

    @Mock
    private TransactionItemRepository transactionItemRepository;

    @Mock
    private GoalItemRepository goalItemRepository;

    @Mock
    private SavingItemRepository savingItemRepository;

    @Mock
    private BudgetItemRepository budgetItemRepository;

    private DashboardServiceImpl service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(
                memberRepository,
                financeSnapshotRepository,
                transactionItemRepository,
                goalItemRepository,
                savingItemRepository,
                budgetItemRepository);
        userId = UUID.fromString("a3333333-3333-4333-8333-333333333333");
    }

    @Test
    void loadReturnsNameAndDashboardNumbers() {
        Member member = new Member();
        member.setId(userId);
        member.setFullName("Aarav Sharma");
        member.setEmail("aarav.sharma@example.com");
        when(memberRepository.findByEmailIgnoreCaseAndDeletedFalse("aarav.sharma@example.com"))
                .thenReturn(Optional.of(member));

        FinanceSnapshot finance = new FinanceSnapshot();
        finance.setMonthlyIncome(new BigDecimal("85000"));
        finance.setMonthlyExpenses(new BigDecimal("42000"));
        finance.setMonthlyBudget(new BigDecimal("40000"));
        when(financeSnapshotRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(finance));

        TransactionItem expense = new TransactionItem();
        expense.setType("expense");
        expense.setAmount(new BigDecimal("4200"));
        expense.setCategory("Food");
        when(transactionItemRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(expense));

        GoalItem goal = new GoalItem();
        goal.setTitle("Emergency fund");
        goal.setStatus("active");
        goal.setTarget(new BigDecimal("200000"));
        goal.setSaved(new BigDecimal("75000"));
        when(goalItemRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        SavingItem saving = new SavingItem();
        saving.setAmount(new BigDecimal("8000"));
        when(savingItemRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(saving));

        BudgetItem budget = new BudgetItem();
        budget.setLimit(new BigDecimal("12000"));
        when(budgetItemRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(budget));

        DashboardData data = service.load(null, "aarav.sharma@example.com");

        assertEquals("Aarav Sharma", data.getUser().getFullName());
        assertEquals(new BigDecimal("85000"), data.getFinance().getMonthlyIncome());
        assertEquals(new BigDecimal("8000"), data.getStats().getTotalSavings());
        assertEquals(new BigDecimal("4200"), data.getStats().getSpent());
        assertEquals(new BigDecimal("40000"), data.getStats().getMonthBudget());
        assertEquals(1, data.getStats().getActiveGoals());
        assertEquals(1, data.getTransactions().size());
    }

    @Test
    void loadRequiresAnEmailOrUserId() {
        assertThrows(ValidationException.class, () -> service.load(null, "  "));
    }

    @Test
    void unknownEmailReturnsNotFound() {
        when(memberRepository.findByEmailIgnoreCaseAndDeletedFalse("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.load(null, "missing@example.com"));
    }
}
