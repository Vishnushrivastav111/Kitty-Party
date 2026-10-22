package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.SavingsRequest;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavingsServiceImplBranchTest {

    @Mock private SavingsRepository savingsRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private FinanceProfileRepository financeProfileRepository;

    @InjectMocks private SavingsServiceImpl savingsService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final List<SavingsEntry> stored = new ArrayList<>();
    private final List<Goal> goals = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(savingsRepository.save(any(SavingsEntry.class))).thenAnswer(invocation -> {
            SavingsEntry entry = invocation.getArgument(0);
            stored.removeIf(existing -> existing.getId().equals(entry.getId()));
            stored.add(entry);
            return entry;
        });
        lenient().when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenAnswer(invocation ->
                stored.stream().filter(entry -> !entry.isDeleted()).toList());
        lenient().when(savingsRepository.findByIdAndUserIdAndDeletedFalse(any(), any())).thenAnswer(invocation -> {
            UUID entryId = invocation.getArgument(0);
            return stored.stream()
                    .filter(entry -> entryId.equals(entry.getId()) && !entry.isDeleted())
                    .findFirst();
        });
        lenient().when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenAnswer(invocation ->
                goals.stream().filter(goal -> !goal.isDeleted()).toList());
        lenient().when(goalRepository.findByIdAndUserIdAndDeletedFalse(any(), any())).thenAnswer(invocation -> {
            UUID goalId = invocation.getArgument(0);
            return goals.stream().filter(goal -> goalId.equals(goal.getId())).findFirst();
        });
    }

    @Test
    void aContributionThatMeetsTheTargetCompletesTheGoal() {
        Goal goal = goal("Laptop", "100", "0", "active");

        SavingsResponse response = savingsService.create(userId, request("Top up", "100", goal.getId(), "Deposit"));

        assertEquals(goal.getId(), response.getGoalId());
        assertEquals(new BigDecimal("100"), goal.getSaved());
        assertEquals("completed", goal.getStatus());
        assertEquals(BigDecimal.ZERO, goal.getPlannedContribution());
    }

    @Test
    void aContributionAboveTheRemainingTargetIsRejected() {
        Goal goal = goal("Laptop", "100", "40", "active");
        stored.add(linked(goal.getId(), "50", "Deposit"));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> savingsService.create(userId, request("Extra", "20", goal.getId(), "Deposit")));

        assertEquals("This contribution exceeds the remaining target of ₹10.00.", exception.getMessage());
    }

    @Test
    void aContributionEqualToTheRemainingTargetIsAccepted() {
        Goal goal = goal("Laptop", "100", "40", "active");
        stored.add(linked(goal.getId(), "50", "Deposit"));

        savingsService.create(userId, request("Last", "10", goal.getId(), "Deposit"));

        assertEquals(new BigDecimal("100"), goal.getSaved());
        assertEquals("completed", goal.getStatus());
    }

    @Test
    void aWithdrawalCanExceedTheRemainingTargetAndCannotDriveSavedNegative() {
        Goal goal = goal("Laptop", "100", "40", "active");
        goal.setPlannedContribution(new BigDecimal("25"));
        stored.add(linked(goal.getId(), "50", "Deposit"));

        savingsService.create(userId, request("Take back", "500", goal.getId(), "Withdrawal"));

        assertEquals(new BigDecimal("0"), goal.getSaved());
        assertEquals("active", goal.getStatus());
        assertEquals(new BigDecimal("25"), goal.getPlannedContribution());
    }

    @Test
    void anArchivedGoalCannotReceiveAContribution() {
        Goal goal = goal("Old", "100", "0", "ARCHIVED");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> savingsService.create(userId, request("Nope", "10", goal.getId(), "Deposit")));

        assertEquals("Archived goals cannot receive new contributions.", exception.getMessage());
    }

    @Test
    void aGoalThatIsNotYoursIsRejected() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> savingsService.create(userId, request("Nope", "10", UUID.randomUUID(), "Deposit")));

        assertEquals("Choose a goal that belongs to you.", exception.getMessage());
    }

    @Test
    void emergencySavingsCannotBeAssignedToAGoal() {
        Goal goal = goal("Laptop", "100", "0", "active");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> savingsService.create(userId, request("Reserve", "10", goal.getId(), " Emergency ")));

        assertEquals("Emergency reserves are not assigned to a goal.", exception.getMessage());
        verify(goalRepository, never()).findByIdAndUserIdAndDeletedFalse(any(), any());
    }

    @Test
    void emergencySavingsWithoutAGoalAreStored() {
        SavingsResponse response = savingsService.create(userId, request("Reserve", "10", null, "emergency"));

        assertEquals("emergency", response.getCategory());
        assertNull(response.getGoalId());
    }

    @Test
    void movingAContributionRefreshesBothGoals() {
        Goal previous = goal("Old", "200", "0", "active");
        Goal next = goal("New", "200", "0", "active");
        SavingsEntry entry = linked(previous.getId(), "30", "Deposit");
        stored.add(entry);

        savingsService.update(userId, entry.getId(), request("Moved", "30", next.getId(), "Deposit"));

        assertEquals(new BigDecimal("0"), previous.getSaved());
        assertEquals(new BigDecimal("30"), next.getSaved());
        verify(goalRepository, times(2)).save(any(Goal.class));
    }

    @Test
    void keepingTheSameGoalRefreshesItOnce() {
        Goal goal = goal("Laptop", "200", "0", "active");
        SavingsEntry entry = linked(goal.getId(), "30", "Deposit");
        stored.add(entry);

        savingsService.update(userId, entry.getId(), request("Same", "40", goal.getId(), "Deposit"));

        assertEquals(new BigDecimal("40"), goal.getSaved());
        verify(goalRepository, times(1)).save(goal);
    }

    @Test
    void clearingAGoalRefreshesOnlyThePreviousOne() {
        Goal previous = goal("Old", "200", "10", "active");
        SavingsEntry entry = linked(previous.getId(), "30", "Deposit");
        stored.add(entry);

        SavingsResponse response = savingsService.update(userId, entry.getId(), request("Free", "30", null, "Deposit"));

        assertNull(response.getGoalId());
        assertEquals(new BigDecimal("10"), previous.getSaved());
        verify(goalRepository, times(1)).save(previous);
    }

    @Test
    void linkingAnUnassignedEntryRefreshesTheNewGoal() {
        Goal next = goal("New", "200", "0", "paused");
        SavingsEntry entry = linked(null, "30", "Deposit");
        stored.add(entry);

        savingsService.update(userId, entry.getId(), request("Link", "30", next.getId(), "Deposit"));

        assertEquals(new BigDecimal("30"), next.getSaved());
        assertEquals("paused", next.getStatus());
        verify(goalRepository, times(1)).save(next);
    }

    @Test
    void anUpdateIgnoresTheEntrysOwnAmountWhenCheckingTheTarget() {
        Goal goal = goal("Laptop", "100", "0", "active");
        SavingsEntry entry = linked(goal.getId(), "80", "Deposit");
        stored.add(entry);

        savingsService.update(userId, entry.getId(), request("Raise", "90", goal.getId(), "Deposit"));

        assertEquals(new BigDecimal("90"), goal.getSaved());
        assertEquals("active", goal.getStatus());
    }

    @Test
    void anotherContributionStillCountsAgainstTheTarget() {
        Goal goal = goal("Laptop", "100", "0", "active");
        SavingsEntry editing = linked(goal.getId(), "40", "Deposit");
        stored.add(editing);
        stored.add(linked(goal.getId(), "30", "Deposit"));
        SavingsEntry ignored = linked(goal.getId(), "1000", "Deposit");
        ignored.setAmount(null);
        stored.add(ignored);
        stored.add(linked(UUID.randomUUID(), "1000", "Deposit"));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> savingsService.update(userId, editing.getId(), request("Too much", "80", goal.getId(), "Deposit")));

        assertEquals("This contribution exceeds the remaining target of ₹70.00.", exception.getMessage());
    }

    @Test
    void savingsAboveTheTargetAreClampedAndTheGoalCompletes() {
        Goal goal = goal("Laptop", "100", "150", "active");
        goal.setPlannedContribution(new BigDecimal("15"));

        savingsService.softDeleteAll(userId);

        assertEquals(new BigDecimal("100"), goal.getSaved());
        assertEquals("completed", goal.getStatus());
        assertEquals(BigDecimal.ZERO, goal.getPlannedContribution());
    }

    @Test
    void aMissingOpeningBalanceCountsAsZero() {
        Goal goal = goal("Laptop", "100", "0", "active");
        goal.setOpeningSaved(null);

        savingsService.create(userId, request("Start", "20", goal.getId(), "Deposit"));

        assertEquals(new BigDecimal("20"), goal.getSaved());
        assertEquals("active", goal.getStatus());
    }

    @Test
    void anArchivedGoalThatIsAlreadyFullStaysArchived() {
        Goal goal = goal("Old", "100", "100", "archived");
        goal.setPlannedContribution(new BigDecimal("15"));
        SavingsEntry entry = linked(goal.getId(), "10", "Deposit");
        stored.add(entry);

        savingsService.update(userId, entry.getId(), request("Detach", "10", null, "Deposit"));

        assertEquals("archived", goal.getStatus());
        assertEquals(new BigDecimal("100"), goal.getSaved());
        assertEquals(new BigDecimal("15"), goal.getPlannedContribution());
    }

    @Test
    void aGoalWithoutATargetIsNotCompletedWhenItsEntryIsRemoved() {
        Goal goal = goal("Open", "1", "10", "paused");
        goal.setTarget(null);
        stored.add(linked(goal.getId(), "5", "Deposit"));

        savingsService.softDelete(userId, stored.get(0).getId());

        assertEquals(new BigDecimal("10"), goal.getSaved());
        assertEquals("paused", goal.getStatus());
    }

    @Test
    void deletingAnEntryRefreshesItsGoal() {
        Goal goal = goal("Laptop", "100", "40", "active");
        SavingsEntry entry = linked(goal.getId(), "30", "Deposit");
        stored.add(entry);

        savingsService.softDelete(userId, entry.getId());

        assertEquals(new BigDecimal("40"), goal.getSaved());
        assertEquals("active", goal.getStatus());
    }

    @Test
    void deletingAnEntryForAMissingGoalDoesNotSaveAGoal() {
        SavingsEntry entry = linked(UUID.randomUUID(), "30", "Deposit");
        stored.add(entry);

        savingsService.softDelete(userId, entry.getId());

        verify(goalRepository, never()).save(any());
    }

    @Test
    void deletingAnUnlinkedEntryDoesNotLookForAGoalToRefresh() {
        SavingsEntry entry = linked(null, "30", "Deposit");
        stored.add(entry);

        savingsService.softDelete(userId, entry.getId());

        verify(goalRepository, never()).save(any());
    }

    @Test
    void deletingEveryEntryRefreshesEveryGoalFromItsOpeningBalance() {
        Goal first = goal("A", "100", "30", "active");
        Goal second = goal("B", "20", "10", "active");
        stored.add(linked(first.getId(), "100", "Deposit"));

        savingsService.softDeleteAll(userId);

        assertEquals(new BigDecimal("30"), first.getSaved());
        assertEquals(new BigDecimal("10"), second.getSaved());
        assertEquals("active", first.getStatus());
        verify(goalRepository, times(2)).save(any(Goal.class));
    }

    private Goal goal(String title, String target, String opening, String status) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setTarget(new BigDecimal(target));
        goal.setOpeningSaved(new BigDecimal(opening));
        goal.setSaved(new BigDecimal(opening));
        goal.setStatus(status);
        goal.setPlannedContribution(new BigDecimal("5"));
        goals.add(goal);
        return goal;
    }

    private SavingsEntry linked(UUID goalId, String amount, String category) {
        SavingsEntry entry = new SavingsEntry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        entry.setTitle("Existing");
        entry.setGoalId(goalId);
        entry.setAmount(new BigDecimal(amount));
        entry.setCategory(category);
        entry.setDate(LocalDate.now());
        return entry;
    }

    private SavingsRequest request(String title, String amount, UUID goalId, String category) {
        SavingsRequest request = new SavingsRequest();
        request.setTitle(title);
        request.setAmount(new BigDecimal(amount));
        request.setDate(LocalDate.now());
        request.setGoalId(goalId);
        request.setCategory(category);
        return request;
    }
}
