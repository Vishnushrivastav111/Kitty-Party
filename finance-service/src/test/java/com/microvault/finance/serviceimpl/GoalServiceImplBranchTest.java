package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.GoalRequest;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.entity.Budget;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplBranchTest {

    @Mock private GoalRepository goalRepository;
    @Mock private SavingsRepository savingsRepository;
    @Mock private FinanceProfileRepository financeProfileRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    @InjectMocks private GoalServiceImpl goalService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private FinanceProfile profile;

    @BeforeEach
    void setUp() {
        lenient().when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        profile = new FinanceProfile();
        profile.setMonthlyIncome(new BigDecimal("100000"));
        profile.setAdditionalIncome(BigDecimal.ZERO);
        profile.setCurrentSavings(new BigDecimal("100000"));
        profile.setEmergencyReserve(BigDecimal.ZERO);
        lenient().when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        lenient().when(financeProfileRepository.save(any(FinanceProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of());
        lenient().when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of());
        lenient().when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(any(Integer.class), any())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenReturn(1);
    }

    @ParameterizedTest
    @MethodSource("statuses")
    void requestedStatusIsNormalised(String raw, String expected) {
        GoalRequest request = request("Trip", "500", "10", raw);
        request.setDeadline(LocalDate.now().plusMonths(6));
        request.setPlannedContribution(new BigDecimal("1000"));

        assertEquals(expected, goalService.create(userId, request).getStatus());
    }

    static Stream<Arguments> statuses() {
        return Stream.of(
                Arguments.of("draft", "draft"),
                Arguments.of("DRAFT", "draft"),
                Arguments.of("active", "active"),
                Arguments.of("ACTIVE", "active"),
                Arguments.of("paused", "paused"),
                Arguments.of("completed", "completed"),
                Arguments.of("archived", "archived"),
                Arguments.of("ARCHIVED", "archived"),
                Arguments.of("", "active"),
                Arguments.of("   ", "active"),
                Arguments.of(null, "active"),
                Arguments.of("whatever", "draft")
        );
    }

    @ParameterizedTest
    @MethodSource("priorities")
    void priorityFallsBackToMedium(String raw, String expected) {
        GoalRequest request = request("Trip", "500", "10", "draft");
        request.setPriority(raw);

        assertEquals(expected, goalService.create(userId, request).getPriority());
    }

    static Stream<Arguments> priorities() {
        return Stream.of(
                Arguments.of("low", "low"),
                Arguments.of("LOW", "low"),
                Arguments.of("high", "high"),
                Arguments.of(" HIGH ", "high"),
                Arguments.of("medium", "medium"),
                Arguments.of(null, "medium"),
                Arguments.of("", "medium"),
                Arguments.of("urgent", "medium")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void createRejectsBadAmounts(String target, String saved, String planned, String message) {
        GoalRequest request = new GoalRequest();
        request.setTitle("Trip");
        request.setTarget(target == null ? null : new BigDecimal(target));
        request.setSaved(saved == null ? null : new BigDecimal(saved));
        request.setPlannedContribution(planned == null ? null : new BigDecimal(planned));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals(message, exception.getMessage());
        verify(goalRepository, never()).save(any());
    }

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(
                Arguments.of(null, "10", null, "Target must be greater than zero"),
                Arguments.of("0", "0", null, "Target must be greater than zero"),
                Arguments.of("-5", "0", null, "Target must be greater than zero"),
                Arguments.of("100", "-1", null, "Saved amount cannot be negative"),
                Arguments.of("100", "150", null, "Saved cannot exceed target"),
                Arguments.of("100", "10", "-1", "Planned contribution cannot be negative")
        );
    }

    @Test
    void blankTextIsClearedAndADraftKeepsItsPlan() {
        GoalRequest request = request("Trip", "500", "10", "draft");
        request.setCategory("   ");
        request.setDescription("  holiday ");
        request.setPlannedContribution(new BigDecimal("40"));

        GoalResponse response = goalService.create(userId, request);

        assertNull(response.getCategory());
        assertEquals("holiday", response.getDescription());
        assertEquals(new BigDecimal("40.00"), response.getPlannedContribution());
    }

    @Test
    void aDraftWithoutAPlanStoresZero() {
        GoalRequest request = request("Trip", "500", "10", "draft");

        assertEquals(new BigDecimal("0.00"), goalService.create(userId, request).getPlannedContribution());
    }

    @Test
    void aFullySavedGoalIsCompletedBeforeItIsActivated() {
        assertEquals("completed", goalService.create(userId, request("Trip", "500", "500", "paused")).getStatus());
    }

    @Test
    void allocatingExistingSavingsMovesOnlyTheUnallocatedIncrease() {
        profile.setCurrentSavings(new BigDecimal("5000"));
        profile.setEmergencyReserve(new BigDecimal("1000"));
        GoalRequest request = request("Trip", "4000", "1000", "draft");
        request.setAllocateExisting(Boolean.TRUE);

        GoalResponse response = goalService.create(userId, request);

        assertEquals(new BigDecimal("4000.00"), profile.getCurrentSavings());
        assertEquals(new BigDecimal("1000"), response.getSaved());
        verify(financeProfileRepository).save(profile);
    }

    @Test
    void leavingAllocateUnsetDoesNotMoveSavings() {
        profile.setCurrentSavings(new BigDecimal("5000"));
        GoalRequest request = request("Trip", "4000", "1000", "draft");
        request.setAllocateExisting(Boolean.FALSE);

        goalService.create(userId, request);

        assertEquals(new BigDecimal("5000"), profile.getCurrentSavings());
        verify(financeProfileRepository, never()).save(any());
    }

    @Test
    void allocatingMoreThanUnallocatedSavingsIsRejected() {
        profile.setCurrentSavings(new BigDecimal("500"));
        profile.setEmergencyReserve(new BigDecimal("2000"));
        GoalRequest request = request("Trip", "4000", "100", "draft");
        request.setAllocateExisting(Boolean.TRUE);

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("Only ₹0.00 of unallocated savings can be assigned. Emergency reserves stay protected.",
                exception.getMessage());
    }

    @Test
    void allocatingWithoutAProfileIsRejected() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        GoalRequest request = request("Trip", "4000", "100", "draft");
        request.setAllocateExisting(Boolean.TRUE);

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("Complete financial setup before allocating existing savings.", exception.getMessage());
    }

    @Test
    void aNonPositiveAllocationDoesNotTouchTheProfile() {
        GoalRequest request = request("Trip", "4000", "0", "draft");
        request.setAllocateExisting(Boolean.TRUE);

        assertEquals("draft", goalService.create(userId, request).getStatus());
        verify(financeProfileRepository, never()).save(any());
    }

    @Test
    void loweringAnOpeningAllocationDoesNotReturnCash() {
        Goal existing = stored("Trip", "5000", "2000");
        existing.setOpeningSaved(new BigDecimal("2000"));
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        GoalRequest request = request("Trip", "5000", "500", "paused");
        request.setAllocateExisting(Boolean.TRUE);

        goalService.update(userId, id, request);

        assertEquals(new BigDecimal("500"), existing.getOpeningSaved());
        verify(financeProfileRepository, never()).save(any());
    }

    @Test
    void laterContributionsCannotPushSavedAboveTheTarget() {
        Goal existing = stored("Trip", "200", "0");
        existing.setOpeningSaved(BigDecimal.ZERO);
        SavingsEntry entry = contribution(id, "150", "Deposit");
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(existing));
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(entry));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> goalService.update(userId, id, request("Trip", "200", "100", "paused")));

        assertEquals("Saved cannot exceed target", exception.getMessage());
    }

    @Test
    void activatingWithoutADeadlineAsksForOne() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> goalService.create(userId, request("Trip", "500", "10", "active")));

        assertEquals("Choose a deadline before activating this goal, or save it as a draft.", exception.getMessage());
    }

    @Test
    void activatingWithoutAProfileAsksForSetup() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        GoalRequest request = request("Trip", "500", "10", "active");
        request.setDeadline(LocalDate.now().plusMonths(6));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("Complete financial setup before activating a goal.", exception.getMessage());
    }

    @Test
    void aPastDeadlineCannotBeActivated() {
        GoalRequest request = request("Trip", "500", "10", "active");
        request.setDeadline(LocalDate.now().minusDays(1));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("The deadline has passed. Extend the deadline, reduce the target, or save the goal as a draft.",
                exception.getMessage());
    }

    @Test
    void aContributionBelowTheRequiredMonthlyIsRejected() {
        GoalRequest request = request("Trip", "12000", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(12));
        request.setPlannedContribution(new BigDecimal("10"));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertTrue(exception.getMessage().contains("below"));
        assertTrue(exception.getMessage().contains("₹1000.00"));
    }

    @Test
    void aContributionAboveTheRemainingCapacityIsRejected() {
        profile.setMonthlyIncome(new BigDecimal("1000"));
        Budget rent = new Budget();
        rent.setLimit(new BigDecimal("800"));
        Budget missingLimit = new Budget();
        missingLimit.setLimit(null);
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(rent, missingLimit));
        Goal paused = otherGoal("paused", "9000");
        Goal activeWithoutPlan = otherGoal("active", null);
        Goal otherActive = otherGoal("ACTIVE", "100");
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(paused, activeWithoutPlan, otherActive));
        GoalRequest request = request("Trip", "6000", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(12));
        request.setPlannedContribution(new BigDecimal("500"));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertTrue(exception.getMessage().contains("₹500.00"));
        assertTrue(exception.getMessage().contains("₹100.00"));
        assertTrue(exception.getMessage().contains("Monthly shortfall"));
    }

    @Test
    void anOmittedPlanUsesTheRequiredMonthlyAmount() {
        GoalRequest request = request("Laptop", "1200", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(12));

        GoalResponse response = goalService.create(userId, request);

        assertEquals(new BigDecimal("100.00"), response.getPlannedContribution());
        assertEquals(new BigDecimal("100.00"), response.getRequiredMonthly());
        assertEquals(12, response.getContributionPeriods());
        assertTrue(response.isFeasible());
        assertEquals("The planned contribution fits the monthly capacity that remains after category budgets.",
                response.getFeasibilityMessage());
    }

    @Test
    void theFourthActiveGoalIsRejected() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(otherGoal("active", "1"), otherGoal("active", "1"), otherGoal("ACTIVE", "1")));
        GoalRequest request = request("Trip", "100", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(6));

        ValidationException exception = assertThrows(ValidationException.class, () -> goalService.create(userId, request));

        assertEquals("You can have at most 3 active goals. Pause, complete, or archive one before activating another. Existing goals are kept.",
                exception.getMessage());
    }

    @Test
    void updatingAnActiveGoalDoesNotCountItselfTowardTheCap() {
        Goal self = stored("Trip", "1000", "0");
        self.setOpeningSaved(BigDecimal.ZERO);
        self.setStatus("active");
        when(goalRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(self));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(self, otherGoal("active", "10"), otherGoal("active", "10")));
        GoalRequest request = request("Trip", "1000", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(6));
        request.setPlannedContribution(new BigDecimal("200"));

        assertEquals("active", goalService.update(userId, id, request).getStatus());
    }

    @Test
    void twoActiveGoalsStillLeaveRoomForOneMore() {
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(otherGoal("active", "10"), otherGoal("paused", "10")));
        GoalRequest request = request("Trip", "100", "0", "active");
        request.setDeadline(LocalDate.now().plusMonths(6));

        assertEquals("active", goalService.create(userId, request).getStatus());
    }

    @Test
    void listUsesOpeningPlusContributionsAndFallsBackToStoredSaved() {
        UUID openingId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID withdrawalId = UUID.randomUUID();
        Goal opening = listed("Open", openingId, "100", "1", "20", "paused");
        Goal entries = listed("Entries", entryId, "100", "77", "0", "paused");
        Goal withdrawn = listed("Down", withdrawalId, "100", "77", "0", "paused");
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(opening, entries, withdrawn));
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(
                contribution(openingId, "5", "Deposit"),
                contribution(entryId, "5", "Deposit"),
                contribution(withdrawalId, "5", "Withdrawal")));

        List<GoalResponse> rows = goalService.list(userId);

        assertEquals(new BigDecimal("25"), rows.get(0).getSaved());
        assertEquals(new BigDecimal("25.00"), rows.get(0).getProgressPercent());
        assertEquals(new BigDecimal("75"), rows.get(0).getRemaining());
        assertEquals(new BigDecimal("5"), rows.get(1).getSaved());
        assertEquals(new BigDecimal("77"), rows.get(2).getSaved());
        assertEquals(new BigDecimal("0.00"), rows.get(0).getPlannedContribution());
    }

    @Test
    void listClampsProgressAndTreatsMissingAmountsAsZero() {
        Goal over = listed("Over", UUID.randomUUID(), "100", "250", "250", "completed");
        Goal empty = new Goal();
        empty.setId(UUID.randomUUID());
        empty.setTitle("Empty");
        empty.setStatus("draft");
        empty.setOpeningSaved(null);
        empty.setSaved(null);
        empty.setTarget(BigDecimal.ZERO);
        empty.setDeadline(null);
        empty.setPlannedContribution(null);
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(over, empty));

        List<GoalResponse> rows = goalService.list(userId);

        assertEquals(new BigDecimal("100.00"), rows.get(0).getProgressPercent());
        assertEquals(new BigDecimal("0.00"), rows.get(0).getRemaining());
        assertEquals(BigDecimal.ZERO, rows.get(1).getSaved());
        assertEquals(new BigDecimal("0.00"), rows.get(1).getProgressPercent());
        assertEquals(0, rows.get(1).getRemaining().compareTo(BigDecimal.ZERO));
        assertNull(rows.get(1).getContributionPeriods());
        assertNull(rows.get(1).getRequiredMonthly());
        assertEquals(new BigDecimal("0.00"), rows.get(1).getPlannedContribution());
    }

    @Test
    void anActiveGoalPastItsDeadlineHasNoRequiredContribution() {
        Goal goal = listed("Late", UUID.randomUUID(), "500", "10", "10", "active");
        goal.setDeadline(LocalDate.now().minusDays(2));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        GoalResponse response = goalService.list(userId).get(0);

        assertNull(response.getRequiredMonthly());
        assertEquals(0, response.getContributionPeriods());
        assertTrue(response.isFeasible());
    }

    @Test
    void anActiveGoalWithoutADeadlineIsStillReportedAsFeasible() {
        Goal goal = listed("Open", UUID.randomUUID(), "500", "10", "10", "active");
        goal.setDeadline(null);
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        GoalResponse response = goalService.list(userId).get(0);

        assertNull(response.getRequiredMonthly());
        assertNull(response.getContributionPeriods());
        assertTrue(response.isFeasible());
        assertEquals("The planned contribution fits the monthly capacity that remains after category budgets.",
                response.getFeasibilityMessage());
    }

    @Test
    void anActiveGoalThatNeedsMoreThanIncomeIsNotFeasible() {
        profile.setMonthlyIncome(new BigDecimal("100"));
        Goal goal = listed("House", UUID.randomUUID(), "100000", "0", "0", "active");
        goal.setDeadline(LocalDate.now().plusMonths(1));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        GoalResponse response = goalService.list(userId).get(0);

        assertFalse(response.isFeasible());
        assertEquals("The required contribution is above the monthly capacity that remains after category budgets.",
                response.getFeasibilityMessage());
    }

    @Test
    void aMissingProfileMakesAnActiveGoalNotFeasible() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        Goal goal = listed("House", UUID.randomUUID(), "1000", "0", "0", "active");
        goal.setDeadline(LocalDate.now().plusMonths(4));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        assertFalse(goalService.list(userId).get(0).isFeasible());
    }

    @Test
    void aPausedGoalDoesNotNeedACapacityCheckToBeFeasible() {
        Goal goal = listed("Pause", UUID.randomUUID(), "1000", "10", "10", "paused");
        goal.setDeadline(LocalDate.now().plusMonths(3));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        GoalResponse response = goalService.list(userId).get(0);

        assertTrue(response.isFeasible());
        assertEquals("The planned contribution fits the monthly capacity that remains after category budgets.",
                response.getFeasibilityMessage());
    }

    @Test
    void goalsCanBeSavedWhenNoDatabaseLockIsAvailable() {
        GoalServiceImpl unlocked = new GoalServiceImpl(
                goalRepository, savingsRepository, financeProfileRepository, budgetRepository, null);

        assertEquals("draft", unlocked.create(userId, request("Trip", "500", "10", "draft")).getStatus());
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void countingEveryGoalWhenNoneIsIgnoredIncludesTheirPlansAndTheActiveCap() throws Exception {
        Goal first = otherGoal("active", "2500");
        Goal second = otherGoal("active", "2500");
        Goal third = otherGoal("active", "2500");
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(first, second, third));
        FinanceProfile income = new FinanceProfile();
        income.setMonthlyIncome(new BigDecimal("10000"));
        Method capacity = GoalServiceImpl.class.getDeclaredMethod(
                "goalCapacity", UUID.class, FinanceProfile.class, UUID.class);
        capacity.setAccessible(true);

        BigDecimal room = (BigDecimal) capacity.invoke(goalService, userId, income, null);

        assertEquals(new BigDecimal("2500.00"), room);
        Method limit = GoalServiceImpl.class.getDeclaredMethod("enforceActiveLimit", UUID.class, UUID.class);
        limit.setAccessible(true);
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> limit.invoke(goalService, userId, null));
        assertInstanceOf(ValidationException.class, thrown.getCause());
        assertEquals("You can have at most 3 active goals. Pause, complete, or archive one before activating another. Existing goals are kept.",
                thrown.getCause().getMessage());
    }

    private GoalRequest request(String title, String target, String saved, String status) {
        GoalRequest request = new GoalRequest();
        request.setTitle(title);
        request.setTarget(new BigDecimal(target));
        request.setSaved(saved == null ? null : new BigDecimal(saved));
        request.setStatus(status);
        return request;
    }

    private Goal stored(String title, String target, String saved) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setTarget(new BigDecimal(target));
        goal.setSaved(new BigDecimal(saved));
        goal.setOpeningSaved(new BigDecimal(saved));
        goal.setStatus("active");
        return goal;
    }

    private Goal otherGoal(String status, String planned) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle(status);
        goal.setStatus(status);
        goal.setTarget(new BigDecimal("100"));
        goal.setSaved(BigDecimal.ZERO);
        goal.setPlannedContribution(planned == null ? null : new BigDecimal(planned));
        return goal;
    }

    private Goal listed(String title, UUID goalId, String target, String saved, String opening, String status) {
        Goal goal = new Goal();
        goal.setId(goalId);
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setTarget(new BigDecimal(target));
        goal.setSaved(new BigDecimal(saved));
        goal.setOpeningSaved(new BigDecimal(opening));
        goal.setStatus(status);
        return goal;
    }

    private SavingsEntry contribution(UUID goalId, String amount, String category) {
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setGoalId(goalId);
        entry.setAmount(new BigDecimal(amount));
        entry.setCategory(category);
        return entry;
    }
}
