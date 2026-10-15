package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.Goal;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.AffordabilityRepository;
import com.microvault.finance.repository.BudgetRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.GoalRepository;
import com.microvault.finance.repository.SavingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffordabilityServiceImplTest {

    @Mock
    private AffordabilityRepository affordabilityRepository;

    @Mock
    private FinanceProfileRepository financeProfileRepository;

    @Mock
    private SavingsRepository savingsRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private AffordabilityServiceImpl affordabilityService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void echoSavedEntity() {
        org.mockito.Mockito.lenient().when(affordabilityRepository.save(any(AffordabilityCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- check: validation

    @Test
    void checkRejectsFutureDateWithoutTouchingRepositories() {
        AffordabilityRequest request = request("Trip", "1000", LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> affordabilityService.check(userId, request));

        assertEquals("Check date cannot be in the future", exception.getMessage());
        verifyNoInteractions(financeProfileRepository, savingsRepository);
        verify(affordabilityRepository, never()).save(any());
    }

    @Test
    void checkAcceptsTodaysDate() {
        stubProfile("10000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Shoes", "1000", LocalDate.now()));

        assertEquals(LocalDate.now(), response.getDate());
        assertEquals("Affordable with planning", response.getVerdict());
    }

    @Test
    void checkAcceptsPastDate() {
        stubProfile("10000", "0");
        stubSavings();

        LocalDate past = LocalDate.now().minusDays(10);
        AffordabilityResponse response = affordabilityService.check(userId, request("Shoes", "1000", past));

        assertEquals(past, response.getDate());
    }

    // ---------------------------------------------------------------- check: profile handling

    @Test
    void checkWithMissingProfileAndNoSavingsHasZeroCapacity() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Phone", "500", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Not currently affordable", response.getVerdict());
        assertEquals("danger", response.getLevel());
        assertTrue(response.getPlan().get(0).contains("income on record is 0.00"));
    }

    @Test
    void checkWithMissingProfileNeverCallsItAffordable() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        stubSavings(savings("10000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Phone", "1000", LocalDate.now()));

        assertEquals(new BigDecimal("10000.00"), response.getAvailable());
        assertEquals(new BigDecimal("10000.00"), response.getAvailableAmount());
        assertEquals("Not currently affordable", response.getVerdict());
    }

    @Test
    void storedExpensesAndEmiAreNotUsedForAffordability() {
        stubProfile("50000", "45000");
        stubSavings(savings("10000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Headphones", "3000", LocalDate.now()));

        assertEquals(new BigDecimal("10000.00"), response.getAvailable());
        assertEquals("Affordable now", response.getVerdict());
        assertEquals("success", response.getLevel());
        assertTrue(response.getPlan().size() >= 4);
    }

    @Test
    void checkWithNullIncomeTreatsIncomeAsZero() {
        FinanceProfile profile = profile(null, new BigDecimal("1000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Desk", "100", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Not currently affordable", response.getVerdict());
        assertTrue(response.getSuggestion().contains("Add your monthly income"));
        assertTrue(response.getPlan().get(0).contains("income on record is 0.00"));
    }

    @Test
    void checkWithZeroIncomeAsksForIncomeEvenWithSavings() {
        stubProfile("0", "0");
        stubSavings(savings("1000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("TV", "150", LocalDate.now()));

        assertEquals(new BigDecimal("1000.00"), response.getAvailable());
        assertEquals("Not currently affordable", response.getVerdict());
        assertEquals("danger", response.getLevel());
    }

    @Test
    void checkWithEmptySavingsListAddsNothing() {
        stubProfile("1000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Affordable with planning", response.getVerdict());
    }

    @Test
    void checkSumsSeveralSavingsEntries() {
        stubProfile("1000", "0");
        stubSavings(savings("1000"), savings("2000"), savings("3000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("6000.00"), response.getAvailable());
        assertEquals("Affordable now", response.getVerdict());
    }

    @Test
    void checkSkipsSavingsEntriesWithNullAmount() {
        stubProfile("1000", "0");
        SavingsEntry broken = savings(null);
        stubSavings(broken, savings("1000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("1000.00"), response.getAvailable());
        assertEquals("Affordable now", response.getVerdict());
    }

    @Test
    void cashThatFitsUnallocatedSavingsIsAffordableNow() {
        FinanceProfile profile = profile(new BigDecimal("10000"), BigDecimal.ZERO);
        profile.setCurrentSavings(new BigDecimal("8000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Item", "3000", LocalDate.now()));

        assertEquals("Affordable now", response.getVerdict());
        assertEquals("success", response.getLevel());
        assertEquals(new BigDecimal("8000.00"), response.getAvailable());
    }

    @Test
    void shortfallWithMonthlyRoomIsAffordableWithPlanning() {
        stubProfile("10000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Item", "3000", LocalDate.now()));

        assertEquals("Affordable with planning", response.getVerdict());
        assertEquals("warning", response.getLevel());
        assertTrue(response.isCanCreateGoal());
        assertTrue(response.getSuggestion().contains("3000.00"));
    }

    @Test
    void budgetsAndActiveGoalsReduceTheMonthlyRoom() {
        stubProfile("10000", "0");
        stubSavings();
        com.microvault.finance.entity.Budget rent = new com.microvault.finance.entity.Budget();
        rent.setLimit(new BigDecimal("6000"));
        com.microvault.finance.entity.Goal goal = new com.microvault.finance.entity.Goal();
        goal.setStatus("active");
        goal.setPlannedContribution(new BigDecimal("3000"));
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(rent));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));

        AffordabilityResponse response = affordabilityService.check(userId, request("Item", "3000", LocalDate.now()));

        // 10000 income - 6000 budgets - 3000 goals = 1000 a month, so 3 months to save 3000
        assertEquals("Affordable with planning", response.getVerdict());
        assertEquals(Integer.valueOf(3), response.getMonthsToSave());
    }

    @Test
    void budgetsAboveIncomeBlockTheCheck() {
        stubProfile("10000", "0");
        stubSavings();
        com.microvault.finance.entity.Budget rent = new com.microvault.finance.entity.Budget();
        rent.setLimit(new BigDecimal("12000"));
        when(budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)).thenReturn(List.of(rent));

        AffordabilityResponse response = affordabilityService.check(userId, request("Item", "3000", LocalDate.now()));

        assertEquals("Not currently affordable", response.getVerdict());
        assertEquals("danger", response.getLevel());
        assertTrue(response.getSuggestion().contains("Reduce a budget"));
    }

    @Test
    void noMonthlyRoomIsNotCurrentlyAffordable() {
        stubProfile("0", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Car", "900000", LocalDate.now()));

        assertEquals("Not currently affordable", response.getVerdict());
        assertEquals("danger", response.getLevel());
    }

    // ---------------------------------------------------------------- check: persistence

    @Test
    void checkPersistsTheCheckWithTrimmedNameAndDefaultPriority() {
        stubProfile("10000", "0");
        stubSavings();
        AffordabilityRequest request = request("  Headphones  ", "1000", LocalDate.now());
        request.setPriority(null);

        AffordabilityResponse response = affordabilityService.check(userId, request);

        ArgumentCaptor<AffordabilityCheck> captor = ArgumentCaptor.forClass(AffordabilityCheck.class);
        verify(affordabilityRepository).save(captor.capture());
        AffordabilityCheck saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals("Headphones", saved.getItemName());
        assertEquals(new BigDecimal("1000"), saved.getAmount());
        assertEquals(new BigDecimal("0.00"), saved.getAvailableAmount());
        assertEquals("Affordable with planning", saved.getVerdict());
        assertEquals("warning", saved.getLevel());
        assertEquals("Want", saved.getPriority());
        assertEquals(LocalDate.now(), saved.getCheckDate());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        assertEquals("Want", response.getPriority());
        assertEquals("Headphones", response.getItemName());
    }

    @Test
    void checkKeepsTheRequestedPriority() {
        stubProfile("10000", "0");
        stubSavings();
        AffordabilityRequest request = request("Laptop", "1000", LocalDate.now());
        request.setPriority("Need");

        AffordabilityResponse response = affordabilityService.check(userId, request);

        assertEquals("Need", response.getPriority());
    }

    @Test
    void checkPropagatesRepositoryFailureOnSave() {
        stubProfile("10000", "0");
        stubSavings();
        when(affordabilityRepository.save(any(AffordabilityCheck.class))).thenThrow(new IllegalStateException("db down"));

        AffordabilityRequest request = request("Laptop", "1000", LocalDate.now());
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> affordabilityService.check(userId, request));

        assertEquals("db down", exception.getMessage());
    }

    @Test
    void checkPropagatesProfileRepositoryFailure() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenThrow(new IllegalStateException("profile db down"));

        AffordabilityRequest request = request("Laptop", "1000", LocalDate.now());
        assertThrows(IllegalStateException.class, () -> affordabilityService.check(userId, request));
        verify(affordabilityRepository, never()).save(any());
    }

    @Test
    void checkPropagatesSavingsRepositoryFailure() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId))
                .thenThrow(new IllegalStateException("savings db down"));

        AffordabilityRequest request = request("Laptop", "1000", LocalDate.now());
        assertThrows(IllegalStateException.class, () -> affordabilityService.check(userId, request));
        verify(affordabilityRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- need vs want savings rules

    @Test
    void wantWithSavingsBelowThreeTimesIncomeHasNothingAvailable() {
        stubProfileWithSavings("50000", "100000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Want", "10000"));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Affordable with planning", response.getVerdict());
        assertEquals(new BigDecimal("150000.00"), response.getProtectedSavings());
    }

    @Test
    void wantWithSavingsExactlyThreeTimesIncomeHasNothingAvailable() {
        stubProfileWithSavings("50000", "150000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Want", "1000"));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertTrue(response.getShortfall().compareTo(new BigDecimal("1000")) == 0);
    }

    @Test
    void wantWithSavingsAboveThreeTimesIncomeUsesOnlyTheExcess() {
        stubProfileWithSavings("50000", "200000", "0");
        stubSavings();

        AffordabilityResponse affordable = affordabilityService.check(userId, requestFor("Want", "40000"));
        assertEquals(new BigDecimal("50000.00"), affordable.getAvailable());
        assertEquals("Affordable now", affordable.getVerdict());

        AffordabilityResponse tooMuch = affordabilityService.check(userId, requestFor("Want", "60000"));
        assertEquals(new BigDecimal("50000.00"), tooMuch.getAvailable());
        assertEquals("Affordable with planning", tooMuch.getVerdict());
        assertEquals(0, new BigDecimal("10000").compareTo(tooMuch.getShortfall()));
        assertTrue(tooMuch.getSuggestion().contains("kept for needs and utilities"));
    }

    @Test
    void wantNeverUsesTheEmergencyReserveInTheThreeTimesCalculation() {
        // 230000 liquid, 30000 of it is the reserve -> 200000 eligible -> 50000 above 150000
        stubProfileWithSavings("50000", "230000", "30000");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Want", "45000"));

        assertEquals(new BigDecimal("50000.00"), response.getAvailable());
        assertEquals(new BigDecimal("30000.00"), response.getEmergencyReserve());
        assertEquals(new BigDecimal("200000.00"), response.getUnallocatedLiquid());
        assertEquals("Affordable now", response.getVerdict());
    }

    @Test
    void needUsesAllEligibleSavingsButNotTheEmergencyReserve() {
        stubProfileWithSavings("50000", "230000", "30000");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Need", "150000"));

        assertEquals(new BigDecimal("200000.00"), response.getAvailable());
        assertEquals(new BigDecimal("0.00"), response.getProtectedSavings());
        assertEquals("Affordable now", response.getVerdict());

        AffordabilityResponse reserveWouldBeNeeded = affordabilityService.check(userId, requestFor("Need", "210000"));
        assertEquals("Affordable with planning", reserveWouldBeNeeded.getVerdict());
        assertEquals(0, new BigDecimal("10000").compareTo(reserveWouldBeNeeded.getShortfall()));
    }

    @Test
    void goalAllocatedSavingsAreNotAvailableAndNothingIsCountedTwice() {
        stubProfileWithSavings("50000", "100000", "20000");
        Goal goal = new Goal();
        goal.setStatus("active");
        goal.setSaved(new BigDecimal("40000"));
        when(goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)).thenReturn(List.of(goal));
        SavingsEntry general = savings("5000");
        SavingsEntry toGoal = savings("7000");
        toGoal.setGoalId(UUID.randomUUID());
        SavingsEntry emergency = savings("3000");
        emergency.setCategory("Emergency");
        SavingsEntry opening = savings("100000");
        opening.setCategory("Opening");
        SavingsEntry withdrawal = savings("2000");
        withdrawal.setCategory("Withdrawal");
        stubSavings(general, toGoal, emergency, opening, withdrawal);

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Need", "1000"));

        // 100000 - 20000 reserve + 5000 general - 2000 withdrawal; goal money, emergency entry and opening mirror excluded
        assertEquals(new BigDecimal("83000.00"), response.getAvailable());
        assertEquals(new BigDecimal("23000.00"), response.getEmergencyReserve());
    }

    @Test
    void missingSavingsGiveZeroForBothPriorities() {
        FinanceProfile profile = profile(new BigDecimal("50000"), BigDecimal.ZERO);
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        stubSavings();

        assertEquals(new BigDecimal("0.00"), affordabilityService.check(userId, requestFor("Need", "100")).getAvailable());
        assertEquals(new BigDecimal("0.00"), affordabilityService.check(userId, requestFor("Want", "100")).getAvailable());
    }

    @Test
    void zeroIncomeDoesNotBreakTheCalculationAndAsksForIncome() {
        stubProfileWithSavings("0", "5000", "0");
        stubSavings();

        AffordabilityResponse want = affordabilityService.check(userId, requestFor("Want", "100"));

        assertEquals(new BigDecimal("5000.00"), want.getAvailable());
        assertEquals(new BigDecimal("0.00"), want.getProtectedSavings());
        assertTrue(want.getSuggestion().contains("Add your monthly income"));
    }

    @Test
    void decimalAmountsKeepTwoDecimalPlaces() {
        stubProfileWithSavings("33333.33", "150000.50", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Want", "100"));

        assertEquals(new BigDecimal("99999.99"), response.getProtectedSavings());
        assertEquals(new BigDecimal("50000.51"), response.getAvailable());
    }

    @Test
    void availableIsNeverNegativeAndInvestmentIsTreatedAsDiscretionary() {
        stubProfileWithSavings("50000", "1000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Investment", "100"));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals(new BigDecimal("150000.00"), response.getProtectedSavings());
    }

    @Test
    void emergencyReserveLargerThanSavingsIsCappedAtTheSavings() {
        stubProfileWithSavings("50000", "10000", "99999");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, requestFor("Need", "100"));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals(new BigDecimal("10000.00"), response.getEmergencyReserve());
    }

    // ---------------------------------------------------------------- history

    @Test
    void listReturnsHistoryNewestFirstAsReturnedByRepository() {
        AffordabilityCheck first = storedCheck("Laptop", LocalDate.of(2026, 3, 2));
        AffordabilityCheck second = storedCheck("Phone", LocalDate.of(2026, 3, 1));
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(first, second));

        List<AffordabilityResponse> history = affordabilityService.list(userId);

        assertEquals(2, history.size());
        assertEquals("Laptop", history.get(0).getItemName());
        assertEquals(first.getId(), history.get(0).getId());
        assertEquals(new BigDecimal("500.00"), history.get(0).getAvailable());
        assertEquals(new BigDecimal("500.00"), history.get(0).getAvailableAmount());
        assertEquals("Comfortably affordable", history.get(0).getVerdict());
        assertEquals("success", history.get(0).getLevel());
        assertEquals("Want", history.get(0).getPriority());
        assertEquals(LocalDate.of(2026, 3, 2), history.get(0).getDate());
        assertEquals("Phone", history.get(1).getItemName());
    }

    @Test
    void listReturnsEmptyHistory() {
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenReturn(List.of());

        assertTrue(affordabilityService.list(userId).isEmpty());
    }

    @Test
    void listPropagatesRepositoryFailure() {
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> affordabilityService.list(userId));
    }

    // ---------------------------------------------------------------- soft delete

    @Test
    void softDeleteMarksTheCheckDeleted() {
        AffordabilityCheck check = storedCheck("Laptop", LocalDate.now());
        when(affordabilityRepository.findByIdAndUserIdAndDeletedFalse(check.getId(), userId))
                .thenReturn(Optional.of(check));

        affordabilityService.softDelete(userId, check.getId());

        assertTrue(check.isDeleted());
        assertNotNull(check.getDeletedAt());
        assertNotNull(check.getUpdatedAt());
        verify(affordabilityRepository).save(check);
    }

    @Test
    void softDeleteOfUnknownCheckThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(affordabilityRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> affordabilityService.softDelete(userId, id));

        assertEquals("Affordability check not found", exception.getMessage());
        verify(affordabilityRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksEveryCheckDeleted() {
        AffordabilityCheck first = storedCheck("Laptop", LocalDate.now());
        AffordabilityCheck second = storedCheck("Phone", LocalDate.now());
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(first, second));

        affordabilityService.softDeleteAll(userId);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        assertNotNull(first.getDeletedAt());
        assertEquals(first.getDeletedAt(), second.getDeletedAt());
        verify(affordabilityRepository, times(2)).save(any(AffordabilityCheck.class));
    }

    @Test
    void softDeleteAllWithNoRecordsSavesNothing() {
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenReturn(List.of());

        affordabilityService.softDeleteAll(userId);

        verify(affordabilityRepository, never()).save(any());
    }

    @Test
    void softDeleteAllPropagatesSaveFailure() {
        AffordabilityCheck first = storedCheck("Laptop", LocalDate.now());
        when(affordabilityRepository.findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId))
                .thenReturn(List.of(first));
        when(affordabilityRepository.save(first)).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> affordabilityService.softDeleteAll(userId));
    }

    // ---------------------------------------------------------------- helpers

    private AffordabilityResponse checkAgainstAvailable(String monthlySurplus, String amount) {
        stubProfile(monthlySurplus, "0");
        stubSavings();
        return affordabilityService.check(userId, request("Item", amount, LocalDate.now()));
    }

    private void stubProfile(String income, String expenses) {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile(new BigDecimal(income), new BigDecimal(expenses))));
    }

    private void stubProfileWithSavings(String income, String liquid, String reserve) {
        FinanceProfile profile = profile(new BigDecimal(income), BigDecimal.ZERO);
        profile.setCurrentSavings(new BigDecimal(liquid));
        profile.setEmergencyReserve(new BigDecimal(reserve));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
    }

    private AffordabilityRequest requestFor(String priority, String amount) {
        AffordabilityRequest request = request("Item", amount, LocalDate.now());
        request.setPriority(priority);
        return request;
    }

    private void stubSavings(SavingsEntry... entries) {
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(entries));
    }

    private static FinanceProfile profile(BigDecimal income, BigDecimal expenses) {
        FinanceProfile profile = new FinanceProfile();
        profile.setMonthlyIncome(income);
        profile.setMonthlyExpenses(expenses);
        return profile;
    }

    private static SavingsEntry savings(String amount) {
        SavingsEntry entry = new SavingsEntry();
        entry.setAmount(amount == null ? null : new BigDecimal(amount));
        return entry;
    }

    private static AffordabilityRequest request(String item, String amount, LocalDate date) {
        AffordabilityRequest request = new AffordabilityRequest();
        request.setItemName(item);
        request.setAmount(new BigDecimal(amount));
        request.setCheckDate(date);
        request.setPriority("Need");
        return request;
    }

    private static AffordabilityCheck storedCheck(String item, LocalDate date) {
        AffordabilityCheck check = new AffordabilityCheck();
        check.setId(UUID.randomUUID());
        check.setItemName(item);
        check.setAmount(new BigDecimal("100"));
        check.setAvailableAmount(new BigDecimal("500.00"));
        check.setVerdict("Comfortably affordable");
        check.setLevel("success");
        check.setPriority("Want");
        check.setCheckDate(date);
        return check;
    }
}
