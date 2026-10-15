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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetOverrunCoverageTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SavingsRepository savingsRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private FinanceProfileRepository financeProfileRepository;
    @Mock private BudgetNotificationRepository notificationRepository;

    private BudgetOverrunCoverage coverage;
    private final UUID userId = UUID.randomUUID();
    private final Budget food = new Budget();
    private final List<SavingsEntry> stored = new ArrayList<>();
    private final List<SavingsEntry> saved = new ArrayList<>();
    private final List<BudgetNotification> notices = new ArrayList<>();
    private final List<Goal> goals = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        coverage = new BudgetOverrunCoverage(budgetRepository, transactionRepository, savingsRepository,
                goalRepository, financeProfileRepository, notificationRepository);
        food.setId(UUID.randomUUID());
        food.setUserId(userId);
        food.setCategory("Food");
        food.setLimit(new BigDecimal("1000"));
        lenient().when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(food));
        lenient().when(transactionRepository.findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(userId)).thenReturn(transactions);
        lenient().when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(stored);
        lenient().when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(goals);
        lenient().when(savingsRepository.save(any(SavingsEntry.class))).thenAnswer(call -> {
            saved.add(call.getArgument(0));
            return call.getArgument(0);
        });
        lenient().when(notificationRepository.saveAndFlush(any(BudgetNotification.class))).thenAnswer(call -> {
            notices.add(call.getArgument(0));
            return call.getArgument(0);
        });
    }

    @Test
    void anOverrunIsTakenFromUnallocatedSavingsFirst() {
        profile("3000", "1000");
        Goal goal = goal("Trip", "1500");
        spend("2500"); // 1,500 over the 1,000 limit

        coverage.reconcile(userId);

        assertEquals(1, saved.size());
        SavingsEntry entry = saved.get(0);
        assertEquals("Withdrawal", entry.getCategory());
        assertEquals(0, new BigDecimal("1500").compareTo(entry.getAmount()));
        assertEquals(null, entry.getGoalId());
        assertEquals(0, new BigDecimal("1500").compareTo(goal.getSaved()));
        assertEquals(1, notices.size());
        assertTrue(notices.get(0).getMessage().contains("from available savings"));
    }

    @Test
    void shortfallMovesOnToGoalsAndThenTheEmergencyReserve() {
        profile("3000", "1000"); // 2,000 unallocated, 1,000 reserve
        Goal goal = goal("Trip", "1500");
        spend("6000"); // 5,000 over

        coverage.reconcile(userId);

        assertEquals(3, saved.size());
        assertEquals("Withdrawal", saved.get(0).getCategory());
        assertEquals(0, new BigDecimal("2000").compareTo(saved.get(0).getAmount()));
        assertEquals(null, saved.get(0).getGoalId());
        assertEquals("Withdrawal", saved.get(1).getCategory());
        assertEquals(0, new BigDecimal("1500").compareTo(saved.get(1).getAmount()));
        assertEquals(goal.getId(), saved.get(1).getGoalId());
        assertEquals("Emergency withdrawal", saved.get(2).getCategory());
        assertEquals(0, new BigDecimal("1000").compareTo(saved.get(2).getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(goal.getSaved()));
        assertTrue(notices.get(0).getMessage().contains("500.00 could not be covered"));
    }

    @Test
    void theEmergencyReserveIsUntouchedWhileOtherSavingsRemain() {
        profile("3000", "1000");
        goal("Trip", "1500");
        spend("3500"); // 2,500 over: 2,000 unallocated + 500 goal

        coverage.reconcile(userId);

        assertEquals(2, saved.size());
        assertEquals(0, new BigDecimal("500").compareTo(saved.get(1).getAmount()));
        assertTrue(saved.stream().noneMatch(entry -> "Emergency withdrawal".equals(entry.getCategory())));
    }

    @Test
    void nothingHappensWithinTheLimit() {
        profile("3000", "1000");
        spend("1000");

        coverage.reconcile(userId);

        assertTrue(saved.isEmpty());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void anAlreadyCoveredOverrunIsNotDeductedTwice() {
        profile("3000", "1000");
        spend("2500");
        stored.add(covered("Withdrawal", "1500", null, 1));

        coverage.reconcile(userId);

        assertTrue(saved.isEmpty());
    }

    @Test
    void onlyTheNewPartOfAGrowingOverrunIsDeducted() {
        profile("3000", "1000");
        spend("3000"); // 2,000 over
        stored.add(covered("Withdrawal", "1500", null, 1));

        coverage.reconcile(userId);

        assertEquals(1, saved.size());
        assertEquals(0, new BigDecimal("500").compareTo(saved.get(0).getAmount()));
    }

    @Test
    void savingsAreGivenBackInReverseOrderWhenTheOverrunShrinks() {
        profile("3000", "1000");
        Goal goal = goal("Trip", "0");
        SavingsEntry fromFree = covered("Withdrawal", "2000", null, 1);
        SavingsEntry fromGoal = covered("Withdrawal", "1500", goal.getId(), 2);
        SavingsEntry fromReserve = covered("Emergency withdrawal", "1000", null, 3);
        stored.add(fromFree);
        stored.add(fromGoal);
        stored.add(fromReserve);
        spend("3000"); // overrun is now 2,000 but 4,500 was covered: give back 2,500

        coverage.reconcile(userId);

        assertTrue(fromReserve.isDeleted());
        assertTrue(fromGoal.isDeleted());
        assertEquals(false, fromFree.isDeleted());
        assertEquals(0, new BigDecimal("1500").compareTo(goal.getSaved()));
    }

    @Test
    void aPartialGiveBackShrinksTheNewestDeduction() {
        profile("3000", "1000");
        SavingsEntry fromFree = covered("Withdrawal", "2000", null, 1);
        SavingsEntry fromReserve = covered("Emergency withdrawal", "1000", null, 2);
        stored.add(fromFree);
        stored.add(fromReserve);
        spend("2700"); // overrun 1,700; covered 3,000; give back 1,300

        coverage.reconcile(userId);

        assertTrue(fromReserve.isDeleted());
        assertEquals(0, new BigDecimal("1700").compareTo(fromFree.getAmount()));
    }

    @Test
    void noSavingsMeansNoDeductionAndNoNotice() {
        spend("2000");

        coverage.reconcile(userId);

        assertTrue(saved.isEmpty());
        assertTrue(notices.isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private void profile(String liquid, String reserve) {
        FinanceProfile profile = new FinanceProfile();
        profile.setCurrentSavings(new BigDecimal(liquid));
        profile.setEmergencyReserve(new BigDecimal(reserve));
        lenient().when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
    }

    private Goal goal(String title, String savedAmount) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setTitle(title);
        goal.setStatus("active");
        goal.setTarget(new BigDecimal("100000"));
        goal.setSaved(new BigDecimal(savedAmount));
        goals.add(goal);
        return goal;
    }

    private void spend(String amount) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setCategory("Food");
        transaction.setType("expense");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(LocalDate.now());
        transactions.add(transaction);
    }

    private SavingsEntry covered(String category, String amount, UUID goalId, int order) {
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        entry.setTitle("Budget overrun: Food");
        entry.setCategory(category);
        entry.setAmount(new BigDecimal(amount));
        entry.setGoalId(goalId);
        entry.setDate(LocalDate.now());
        entry.setNote(BudgetOverrunCoverage.TAG_PREFIX + food.getId() + ":" + YearMonth.now());
        entry.setCreatedAt(LocalDateTime.now().minusMinutes(10).plusSeconds(order));
        return entry;
    }
}
