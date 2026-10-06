package com.microvault.finance.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.dto.BudgetResponse;
import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.GoalResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.dto.SavingsResponse;
import com.microvault.finance.dto.TransactionResponse;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.service.AffordabilityService;
import com.microvault.finance.service.BudgetService;
import com.microvault.finance.service.GoalService;
import com.microvault.finance.service.ReportService;
import com.microvault.finance.service.SavingsService;
import com.microvault.finance.service.SetupSkipStore;
import com.microvault.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProfileServiceImplTest {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    @Mock
    private FinanceProfileRepository financeProfileRepository;
    @Mock
    private SetupSkipStore setupSkipStore;
    @Mock
    private TransactionService transactionService;
    @Mock
    private GoalService goalService;
    @Mock
    private SavingsService savingsService;
    @Mock
    private BudgetService budgetService;
    @Mock
    private ReportService reportService;
    @Mock
    private AffordabilityService affordabilityService;

    @InjectMocks
    private FinanceProfileServiceImpl service;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void echoSavedEntity() {
        lenient().when(financeProfileRepository.save(any(FinanceProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- saveForUser: create / update

    @Test
    void saveCreatesAProfileWhenNoneExists() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = fullRequest();

        FinanceProfileResponse response = service.saveForUser(userId, request);

        ArgumentCaptor<FinanceProfile> captor = ArgumentCaptor.forClass(FinanceProfile.class);
        verify(financeProfileRepository).save(captor.capture());
        FinanceProfile saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertFalse(saved.isDeleted());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("Salary", saved.getIncomeSource());
        assertEquals(new BigDecimal("50000"), saved.getMonthlyIncome());
        assertEquals("Monthly", saved.getPayCycle());
        assertEquals(new BigDecimal("20000"), saved.getMonthlyExpenses());
        assertEquals("Rent, Food", saved.getExpenseCategories());
        assertTrue(saved.isHasLoan());
        assertEquals("Home", saved.getLoanType());
        assertEquals(new BigDecimal("900000"), saved.getLoanAmount());
        assertEquals(new BigDecimal("8000"), saved.getMonthlyEmi());
        assertEquals(LocalDate.of(2025, 1, 5), saved.getEmiStartDate());
        assertEquals(new BigDecimal("15000"), saved.getCurrentSavings());
        assertEquals("Bank", saved.getSavingsType());
        assertEquals(new BigDecimal("7000"), saved.getInvestments());
        assertEquals("Stocks", saved.getInvestmentTypes());
        assertEquals(new BigDecimal("18000"), saved.getMonthlyBudget());
        assertEquals("50-30-20", saved.getBudgetStyle());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getSetupDate());
        verify(setupSkipStore).clear(userId);

        assertEquals(saved.getId(), response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals("Salary", response.getIncomeSource());
        assertEquals(new BigDecimal("50000"), response.getMonthlyIncome());
        assertEquals("Monthly", response.getPayCycle());
        assertEquals(new BigDecimal("20000"), response.getMonthlyExpenses());
        assertEquals("Rent, Food", response.getExpenseCategories());
        assertTrue(response.isHasLoan());
        assertEquals("Home", response.getLoanType());
        assertEquals(new BigDecimal("900000"), response.getLoanAmount());
        assertEquals(new BigDecimal("8000"), response.getMonthlyEmi());
        assertEquals(LocalDate.of(2025, 1, 5), response.getEmiStartDate());
        assertEquals(new BigDecimal("15000"), response.getCurrentSavings());
        assertEquals("Bank", response.getSavingsType());
        assertEquals(new BigDecimal("7000"), response.getInvestments());
        assertEquals("Stocks", response.getInvestmentTypes());
        assertEquals(new BigDecimal("18000"), response.getMonthlyBudget());
        assertEquals("50-30-20", response.getBudgetStyle());
        assertEquals(LocalDate.of(2026, 1, 1), response.getSetupDate());
    }

    @Test
    void saveUpdatesTheExistingProfileAndKeepsItsIdentity() {
        FinanceProfile existing = new FinanceProfile();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setUserId(userId);
        existing.setCreatedAt(java.time.LocalDateTime.of(2025, 1, 1, 0, 0));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(existing));

        FinanceProfileResponse response = service.saveForUser(userId, minimalRequest("1000"));

        assertSame(existing, captureSaved());
        assertEquals(existingId, response.getId());
        assertEquals(java.time.LocalDateTime.of(2025, 1, 1, 0, 0), existing.getCreatedAt());
        assertEquals(new BigDecimal("1000"), existing.getMonthlyIncome());
        assertNotNull(existing.getUpdatedAt());
        verify(setupSkipStore).clear(userId);
    }

    @Test
    void saveWithOnlyIncomeDefaultsEverythingElseToZeroOrNull() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        service.saveForUser(userId, minimalRequest("1000"));

        FinanceProfile saved = captureSaved();
        assertEquals(BigDecimal.ZERO, saved.getMonthlyExpenses());
        assertEquals(BigDecimal.ZERO, saved.getLoanAmount());
        assertEquals(BigDecimal.ZERO, saved.getMonthlyEmi());
        assertEquals(BigDecimal.ZERO, saved.getCurrentSavings());
        assertEquals(BigDecimal.ZERO, saved.getInvestments());
        assertEquals(BigDecimal.ZERO, saved.getMonthlyBudget());
        assertFalse(saved.isHasLoan());
        assertNull(saved.getIncomeSource());
        assertNull(saved.getPayCycle());
        assertNull(saved.getExpenseCategories());
        assertNull(saved.getInvestmentTypes());
        assertNull(saved.getLoanType());
        assertNull(saved.getSetupDate());
        assertNull(saved.getEmiStartDate());
    }

    @Test
    void savePropagatesRepositoryExceptionOnSave() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(financeProfileRepository.save(any(FinanceProfile.class))).thenThrow(new IllegalStateException("db down"));

        FinanceProfileRequest request = minimalRequest("1000");
        assertThrows(IllegalStateException.class, () -> service.saveForUser(userId, request));
    }

    @Test
    void savePropagatesRepositoryExceptionOnLookup() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenThrow(new IllegalStateException("db down"));

        FinanceProfileRequest request = minimalRequest("1000");
        assertThrows(IllegalStateException.class, () -> service.saveForUser(userId, request));
        verify(financeProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- saveForUser: validation

    @Test
    void saveRejectsNullRequest() {
        ValidationException exception = assertThrows(ValidationException.class, () -> service.saveForUser(userId, null));

        assertEquals("Monthly income is required", exception.getMessage());
        verify(financeProfileRepository, never()).save(any());
    }

    @Test
    void saveRejectsMissingIncome() {
        FinanceProfileRequest request = new FinanceProfileRequest();

        ValidationException exception = assertThrows(ValidationException.class, () -> service.saveForUser(userId, request));

        assertEquals("Monthly income is required", exception.getMessage());
    }

    @Test
    void saveRejectsNegativeIncome() {
        assertValidationMessage(minimalRequest("-1"), "Monthly income cannot be negative");
    }

    @Test
    void saveRejectsNegativeExpenses() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setMonthlyExpenses(new BigDecimal("-1"));
        assertValidationMessage(request, "Monthly expenses cannot be negative");
    }

    @Test
    void saveRejectsNegativeCurrentSavings() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setCurrentSavings(new BigDecimal("-1"));
        assertValidationMessage(request, "Current savings cannot be negative");
    }

    @Test
    void saveRejectsNegativeMonthlyBudget() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setMonthlyBudget(new BigDecimal("-1"));
        assertValidationMessage(request, "Monthly budget cannot be negative");
    }

    @Test
    void saveRejectsNegativeLoanAmount() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setLoanAmount(new BigDecimal("-1"));
        assertValidationMessage(request, "Loan amount cannot be negative");
    }

    @Test
    void saveRejectsNegativeMonthlyEmi() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setMonthlyEmi(new BigDecimal("-1"));
        assertValidationMessage(request, "Monthly EMI cannot be negative");
    }

    @Test
    void saveRejectsNegativeLoansEmiAlias() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setLoansEmi(new BigDecimal("-1"));
        assertValidationMessage(request, "Monthly EMI cannot be negative");
    }

    @Test
    void saveRejectsNegativeInvestments() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setInvestments(new BigDecimal("-1"));
        assertValidationMessage(request, "Investments cannot be negative");
    }

    @Test
    void saveRejectsFutureSetupDate() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setSetupDate(LocalDate.now().plusDays(1).toString());
        assertValidationMessage(request, "Setup date cannot be in the future");
    }

    @Test
    void saveAcceptsTodayAsSetupDate() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setSetupDate(" " + LocalDate.now() + " ");

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertEquals(LocalDate.now(), response.getSetupDate());
    }

    @Test
    void saveTreatsBlankDatesAsMissing() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setSetupDate("   ");
        request.setEmiStartDate("");

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertNull(response.getSetupDate());
        assertNull(response.getEmiStartDate());
    }

    @Test
    void saveRejectsUnparseableDate() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setSetupDate("01/02/2026");

        assertThrows(DateTimeParseException.class, () -> service.saveForUser(userId, request));
        verify(financeProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- saveForUser: EMI alias

    @Test
    void saveUsesMonthlyEmiWhenBothEmiFieldsAreGiven() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setMonthlyEmi(new BigDecimal("300"));
        request.setLoansEmi(new BigDecimal("999"));

        assertEquals(new BigDecimal("300"), service.saveForUser(userId, request).getMonthlyEmi());
    }

    @Test
    void saveFallsBackToLoansEmiWhenMonthlyEmiIsMissing() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setLoansEmi(new BigDecimal("450"));

        assertEquals(new BigDecimal("450"), service.saveForUser(userId, request).getMonthlyEmi());
    }

    // ---------------------------------------------------------------- saveForUser: JSON helpers

    @Test
    void hasLoanIsFalseForNullJavaValueAndJsonNull() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertFalse(saveWithHasLoan(null).isHasLoan());
        assertFalse(saveWithHasLoan(NullNode.getInstance()).isHasLoan());
    }

    @Test
    void hasLoanReadsBooleanNodes() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertTrue(saveWithHasLoan(BooleanNode.TRUE).isHasLoan());
        assertFalse(saveWithHasLoan(BooleanNode.FALSE).isHasLoan());
    }

    @Test
    void hasLoanReadsYesAndTrueTextCaseInsensitively() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertTrue(saveWithHasLoan(TextNode.valueOf("Yes")).isHasLoan());
        assertTrue(saveWithHasLoan(TextNode.valueOf("TRUE")).isHasLoan());
        assertFalse(saveWithHasLoan(TextNode.valueOf("no")).isHasLoan());
        assertFalse(saveWithHasLoan(TextNode.valueOf("")).isHasLoan());
        assertFalse(saveWithHasLoan(IntNode.valueOf(1)).isHasLoan());
    }

    @Test
    void listFieldsAreJoinedFromArrays() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        ArrayNode categories = JSON.arrayNode().add("Rent").add("").add("Food");
        request.setExpenseCategories(categories);

        assertEquals("Rent, , Food", service.saveForUser(userId, request).getExpenseCategories());
    }

    @Test
    void listFieldsIgnoreArraysWithOnlyEmptyItems() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setExpenseCategories(JSON.arrayNode());
        request.setInvestmentTypes(JSON.arrayNode().add(""));

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertNull(response.getExpenseCategories());
        assertNull(response.getInvestmentTypes());
    }

    @Test
    void listFieldsAcceptPlainTextAndIgnoreBlankOrNull() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setExpenseCategories(TextNode.valueOf("  Rent  "));
        request.setInvestmentTypes(NullNode.getInstance());

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertEquals("Rent", response.getExpenseCategories());
        assertNull(response.getInvestmentTypes());

        FinanceProfileRequest blank = minimalRequest("1000");
        blank.setExpenseCategories(TextNode.valueOf("   "));
        assertNull(service.saveForUser(userId, blank).getExpenseCategories());
    }

    @Test
    void textFieldsAreTrimmedAndBlankBecomesNull() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setIncomeSource("  Freelance ");
        request.setPayCycle("   ");

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertEquals("Freelance", response.getIncomeSource());
        assertNull(response.getPayCycle());
    }

    // ---------------------------------------------------------------- getByUserId

    @Test
    void getByUserIdReturnsProfile() {
        FinanceProfile profile = profile("50000", "20000", "5000");
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));

        FinanceProfileResponse response = service.getByUserId(userId);

        assertEquals(new BigDecimal("50000"), response.getMonthlyIncome());
        assertEquals(userId, response.getUserId());
    }

    @Test
    void getByUserIdThrowsNotFoundWhenNoProfile() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getByUserId(userId));

        assertEquals("No active financial profile found for this user", exception.getMessage());
    }

    // ---------------------------------------------------------------- summary

    @Test
    void summaryComputesSurplusAndSavingsRate() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile("50000", "20000", "5000")));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(userId, summary.getUserId());
        assertEquals(new BigDecimal("50000"), summary.getMonthlyIncome());
        assertEquals(new BigDecimal("20000"), summary.getMonthlyExpenses());
        assertEquals(new BigDecimal("5000"), summary.getMonthlyEmi());
        assertEquals(new BigDecimal("30000"), summary.getMonthlySurplus());
        assertEquals(new BigDecimal("60.00"), summary.getSavingsRatePercent());
    }

    @Test
    void summaryClampsNegativeSurplusToZero() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile("10000", "15000", "0")));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(BigDecimal.ZERO, summary.getMonthlySurplus());
        assertEquals(new BigDecimal("0.00"), summary.getSavingsRatePercent());
    }

    @Test
    void summaryWithZeroIncomeHasZeroRate() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile("0", "0", "0")));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(BigDecimal.ZERO, summary.getSavingsRatePercent());
        assertEquals(0, summary.getMonthlySurplus().signum());
    }

    @Test
    void summaryTreatsNullAmountsAsZero() {
        FinanceProfile profile = new FinanceProfile();
        profile.setUserId(userId);
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(BigDecimal.ZERO, summary.getMonthlyIncome());
        assertEquals(BigDecimal.ZERO, summary.getMonthlyExpenses());
        assertEquals(BigDecimal.ZERO, summary.getMonthlyEmi());
        assertEquals(BigDecimal.ZERO, summary.getSavingsRatePercent());
    }

    @Test
    void summaryThrowsNotFoundWhenNoProfile() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.summary(userId));
    }

    // ---------------------------------------------------------------- skip / active users

    @Test
    void skipSetupRemembersTheUser() {
        service.skipSetup(userId);

        verify(setupSkipStore).skip(userId);
    }

    @Test
    void activeUserIdsListsEveryActiveProfileOwner() {
        UUID other = UUID.randomUUID();
        FinanceProfile first = new FinanceProfile();
        first.setUserId(userId);
        FinanceProfile second = new FinanceProfile();
        second.setUserId(other);
        when(financeProfileRepository.findByDeletedFalse()).thenReturn(List.of(first, second));

        assertEquals(List.of(userId, other), service.activeUserIds());
    }

    @Test
    void activeUserIdsIsEmptyWhenThereAreNoProfiles() {
        when(financeProfileRepository.findByDeletedFalse()).thenReturn(List.of());

        assertTrue(service.activeUserIds().isEmpty());
    }

    // ---------------------------------------------------------------- workspace

    @Test
    void workspaceCollectsProfileAndEverySection() {
        FinanceProfile profile = profile("50000", "20000", "0");
        List<TransactionResponse> transactions = List.of(new TransactionResponse());
        List<GoalResponse> goals = List.of(new GoalResponse());
        List<SavingsResponse> savings = List.of(new SavingsResponse());
        List<BudgetResponse> budgets = List.of(new BudgetResponse());
        List<ReportResponse> reports = List.of(new ReportResponse());
        List<AffordabilityResponse> checks = List.of(new AffordabilityResponse());
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        when(transactionService.list(userId)).thenReturn(transactions);
        when(goalService.list(userId)).thenReturn(goals);
        when(savingsService.list(userId)).thenReturn(savings);
        when(budgetService.list(userId)).thenReturn(budgets);
        when(reportService.list(userId)).thenReturn(reports);
        when(affordabilityService.list(userId)).thenReturn(checks);

        FinanceWorkspaceResponse workspace = service.workspace(userId);

        assertNotNull(workspace.getFinance());
        assertEquals(new BigDecimal("50000"), workspace.getFinance().getMonthlyIncome());
        assertSame(transactions, workspace.getTransactions());
        assertSame(goals, workspace.getGoals());
        assertSame(savings, workspace.getSavings());
        assertSame(budgets, workspace.getBudgets());
        assertSame(reports, workspace.getReports());
        assertSame(checks, workspace.getAffordChecks());
        assertFalse(workspace.isSetupSkipped());
        verify(setupSkipStore, never()).isSkipped(any());
    }

    @Test
    void workspaceWithoutProfileReportsSkippedSetup() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(setupSkipStore.isSkipped(userId)).thenReturn(true);

        FinanceWorkspaceResponse workspace = service.workspace(userId);

        assertNull(workspace.getFinance());
        assertTrue(workspace.isSetupSkipped());
        assertTrue(workspace.getTransactions().isEmpty());
    }

    @Test
    void workspaceWithoutProfileAndWithoutSkipIsNotSkipped() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(setupSkipStore.isSkipped(userId)).thenReturn(false);

        FinanceWorkspaceResponse workspace = service.workspace(userId);

        assertNull(workspace.getFinance());
        assertFalse(workspace.isSetupSkipped());
    }

    @Test
    void workspacePropagatesSectionServiceFailure() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(transactionService.list(userId)).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.workspace(userId));
    }

    // ---------------------------------------------------------------- helpers

    private void assertValidationMessage(FinanceProfileRequest request, String message) {
        ValidationException exception = assertThrows(ValidationException.class, () -> service.saveForUser(userId, request));
        assertEquals(message, exception.getMessage());
        verify(financeProfileRepository, never()).save(any());
    }

    private FinanceProfile captureSaved() {
        ArgumentCaptor<FinanceProfile> captor = ArgumentCaptor.forClass(FinanceProfile.class);
        verify(financeProfileRepository).save(captor.capture());
        return captor.getValue();
    }

    private FinanceProfileResponse saveWithHasLoan(JsonNode node) {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setHasLoan(node);
        return service.saveForUser(userId, request);
    }

    private FinanceProfileRequest minimalRequest(String income) {
        FinanceProfileRequest request = new FinanceProfileRequest();
        request.setMonthlyIncome(new BigDecimal(income));
        return request;
    }

    private FinanceProfileRequest fullRequest() {
        FinanceProfileRequest request = minimalRequest("50000");
        request.setIncomeSource("  Salary ");
        request.setPayCycle("Monthly");
        request.setMonthlyExpenses(new BigDecimal("20000"));
        request.setExpenseCategories(JSON.arrayNode().add("Rent").add("Food"));
        request.setHasLoan(BooleanNode.TRUE);
        request.setLoanType("Home");
        request.setLoanAmount(new BigDecimal("900000"));
        request.setMonthlyEmi(new BigDecimal("8000"));
        request.setEmiStartDate("2025-01-05");
        request.setCurrentSavings(new BigDecimal("15000"));
        request.setSavingsType("Bank");
        request.setInvestments(new BigDecimal("7000"));
        request.setInvestmentTypes(JSON.arrayNode().add("Stocks"));
        request.setMonthlyBudget(new BigDecimal("18000"));
        request.setBudgetStyle("50-30-20");
        request.setSetupDate("2026-01-01");
        return request;
    }

    private FinanceProfile profile(String income, String expenses, String emi) {
        FinanceProfile profile = new FinanceProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setMonthlyIncome(new BigDecimal(income));
        profile.setMonthlyExpenses(new BigDecimal(expenses));
        profile.setMonthlyEmi(new BigDecimal(emi));
        return profile;
    }
}
