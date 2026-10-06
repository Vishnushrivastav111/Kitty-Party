package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.AffordabilityRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
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
        assertEquals("Comfortably affordable", response.getVerdict());
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
        assertEquals("Tight — consider delaying", response.getVerdict());
        assertEquals("warning", response.getLevel());
        assertTrue(response.getPlan().get(0).contains("income 0 minus expenses 0"));
    }

    @Test
    void checkWithMissingProfileUsesOnlySavingsShare() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        stubSavings(savings("10000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Phone", "1000", LocalDate.now()));

        assertEquals(new BigDecimal("3000.00"), response.getAvailable());
        assertEquals(new BigDecimal("3000.00"), response.getAvailableAmount());
        assertEquals("Affordable with caution", response.getVerdict());
    }

    @Test
    void checkWithValidProfileAddsSurplusAndSavingsShare() {
        stubProfile("50000", "20000");
        stubSavings(savings("10000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Headphones", "3000", LocalDate.now()));

        assertEquals(new BigDecimal("33000.00"), response.getAvailable());
        assertEquals("Comfortably affordable", response.getVerdict());
        assertEquals("success", response.getLevel());
        assertEquals(4, response.getPlan().size());
    }

    @Test
    void checkWithNullIncomeTreatsIncomeAsZero() {
        FinanceProfile profile = profile(null, new BigDecimal("1000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Desk", "100", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertTrue(response.getPlan().get(0).contains("income 0 minus expenses 1000"));
    }

    @Test
    void checkWithNullExpensesTreatsExpensesAsZero() {
        FinanceProfile profile = profile(new BigDecimal("4000"), null);
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Desk", "400", LocalDate.now()));

        assertEquals(new BigDecimal("4000.00"), response.getAvailable());
        assertTrue(response.getPlan().get(0).contains("income 4000 minus expenses 0"));
    }

    @Test
    void checkWithNullIncomeAndNullExpensesHasNoCapacity() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile(null, null)));
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Desk", "100", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Tight — consider delaying", response.getVerdict());
    }

    @Test
    void checkWithNegativeSurplusClampsSurplusToZeroButKeepsSavingsShare() {
        stubProfile("20000", "30000");
        stubSavings(savings("1000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("TV", "150", LocalDate.now()));

        assertEquals(new BigDecimal("300.00"), response.getAvailable());
        assertEquals("Affordable with caution", response.getVerdict());
    }

    @Test
    void checkWithNegativeSurplusAndNoSavingsIsZeroCapacity() {
        stubProfile("20000", "30000");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("TV", "150", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Tight — consider delaying", response.getVerdict());
        assertEquals("warning", response.getLevel());
    }

    @Test
    void checkWithZeroSurplusIsZeroCapacity() {
        stubProfile("20000", "20000");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("TV", "150", LocalDate.now()));

        assertEquals(new BigDecimal("0.00"), response.getAvailable());
        assertEquals("Tight — consider delaying", response.getVerdict());
    }

    // ---------------------------------------------------------------- check: savings handling

    @Test
    void checkWithEmptySavingsListAddsNothing() {
        stubProfile("1000", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("1000.00"), response.getAvailable());
    }

    @Test
    void checkSumsSeveralSavingsEntries() {
        stubProfile("1000", "0");
        stubSavings(savings("1000"), savings("2000"), savings("3000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("2800.00"), response.getAvailable());
        assertTrue(response.getPlan().get(1).contains("(6000)"));
    }

    @Test
    void checkSkipsSavingsEntriesWithNullAmount() {
        stubProfile("1000", "0");
        SavingsEntry broken = savings(null);
        stubSavings(broken, savings("1000"));

        AffordabilityResponse response = affordabilityService.check(userId, request("Book", "100", LocalDate.now()));

        assertEquals(new BigDecimal("1300.00"), response.getAvailable());
    }

    // ---------------------------------------------------------------- check: ratio verdicts

    @Test
    void ratioExactlyThirtyPercentIsComfortable() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "3000");

        assertEquals("Comfortably affordable", response.getVerdict());
        assertEquals("success", response.getLevel());
        assertTrue(response.getSuggestion().startsWith("Buy now."));
    }

    @Test
    void ratioBelowThirtyPercentIsComfortable() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "100");

        assertEquals("Comfortably affordable", response.getVerdict());
        assertEquals("success", response.getLevel());
    }

    @Test
    void ratioJustAboveThirtyPercentIsAffordableWithCaution() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "3001");

        assertEquals("Affordable with caution", response.getVerdict());
        assertEquals("warning", response.getLevel());
        assertTrue(response.getSuggestion().startsWith("You can buy this"));
    }

    @Test
    void ratioExactlySixtyPercentIsAffordableWithCaution() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "6000");

        assertEquals("Affordable with caution", response.getVerdict());
        assertEquals("warning", response.getLevel());
    }

    @Test
    void ratioJustAboveSixtyPercentIsTight() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "6001");

        assertEquals("Tight — consider delaying", response.getVerdict());
        assertEquals("warning", response.getLevel());
        assertTrue(response.getSuggestion().startsWith("Delay this purchase."));
    }

    @Test
    void ratioExactlyOneHundredPercentIsTight() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "10000");

        assertEquals("Tight — consider delaying", response.getVerdict());
        assertEquals("warning", response.getLevel());
    }

    @Test
    void ratioAboveOneHundredPercentIsNotRecommendedWithShortfall() {
        AffordabilityResponse response = checkAgainstAvailable("10000", "10001");

        assertEquals("Not recommended", response.getVerdict());
        assertEquals("danger", response.getLevel());
        assertEquals("Do not buy this now. You are short by 1.00. Save that amount before you purchase.",
                response.getSuggestion());
    }

    @Test
    void notRecommendedWhenThereIsNoCapacityAtAll() {
        // zero capacity pins the ratio to 100%, so it is "tight" rather than "not recommended"
        stubProfile("0", "0");
        stubSavings();

        AffordabilityResponse response = affordabilityService.check(userId, request("Car", "900000", LocalDate.now()));

        assertEquals("Tight — consider delaying", response.getVerdict());
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
        assertEquals(new BigDecimal("10000.00"), saved.getAvailableAmount());
        assertEquals("Comfortably affordable", saved.getVerdict());
        assertEquals("success", saved.getLevel());
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
        AffordabilityResponse response = affordabilityService.check(userId, request("Item", amount, LocalDate.now()));
        assertFalse(response.getPlan().isEmpty());
        assertEquals(new BigDecimal(monthlySurplus).setScale(2), response.getAvailable());
        return response;
    }

    private void stubProfile(String income, String expenses) {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile(new BigDecimal(income), new BigDecimal(expenses))));
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
        request.setPriority("Want");
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
