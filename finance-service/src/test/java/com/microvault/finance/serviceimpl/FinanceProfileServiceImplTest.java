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
        lenient().when(goalService.list(any())).thenReturn(List.of());
        lenient().when(budgetService.list(any())).thenReturn(List.of());
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
        assertEquals(new BigDecimal("5000"), saved.getAdditionalIncome());
        assertEquals(new BigDecimal("15000"), saved.getCurrentSavings());
        assertEquals(new BigDecimal("6000"), saved.getEmergencyReserve());
        assertEquals("Bank", saved.getSavingsType());
        assertEquals(new BigDecimal("7000"), saved.getInvestments());
        assertEquals("Stocks", saved.getInvestmentTypes());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getSetupDate());
        verify(setupSkipStore).clear(userId);

        assertEquals(saved.getId(), response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals("Salary", response.getIncomeSource());
        assertEquals(new BigDecimal("50000"), response.getMonthlyIncome());
        assertEquals("Monthly", response.getPayCycle());
        assertEquals(new BigDecimal("15000"), response.getCurrentSavings());
        assertEquals("Bank", response.getSavingsType());
        assertEquals(new BigDecimal("7000"), response.getInvestments());
        assertEquals("Stocks", response.getInvestmentTypes());
        assertEquals(LocalDate.of(2026, 1, 1), response.getSetupDate());
    }

    @Test
    void saveIgnoresRetiredExpenseLoanAndBudgetFields() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setMonthlyExpenses(new BigDecimal("20000"));
        request.setOtherCommitments(new BigDecimal("500"));
        request.setHasLoan(BooleanNode.TRUE);
        request.setLoanAmount(new BigDecimal("900000"));
        request.setMonthlyEmi(new BigDecimal("8000"));
        request.setMonthlyBudget(new BigDecimal("18000"));

        service.saveForUser(userId, request);

        FinanceProfile saved = captureSaved();
        assertEquals(BigDecimal.ZERO, saved.getMonthlyExpenses());
        assertEquals(BigDecimal.ZERO, saved.getOtherCommitments());
        assertEquals(BigDecimal.ZERO, saved.getLoanAmount());
        assertEquals(BigDecimal.ZERO, saved.getMonthlyEmi());
        assertEquals(BigDecimal.ZERO, saved.getMonthlyBudget());
        assertFalse(saved.isHasLoan());
    }

    @Test
    void savingAgainKeepsStoredLegacyValuesUntouched() {
        FinanceProfile existing = profile("1000", "20000", "5000");
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(existing));

        service.saveForUser(userId, minimalRequest("2000"));

        assertEquals(new BigDecimal("2000"), existing.getMonthlyIncome());
        assertEquals(new BigDecimal("20000"), existing.getMonthlyExpenses());
        assertEquals(new BigDecimal("5000"), existing.getMonthlyEmi());
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
        assertEquals(LocalDate.now(), saved.getSetupDate());
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
    void saveRejectsZeroIncome() {
        assertValidationMessage(minimalRequest("0"), "Monthly income must be greater than zero");
    }

    @Test
    void saveRejectsEmergencyReserveAboveLiquidSavings() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setCurrentSavings(new BigDecimal("500"));
        request.setEmergencyReserve(new BigDecimal("501"));
        assertValidationMessage(request, "Emergency reserve cannot be larger than current liquid savings");
    }

    @Test
    void saveRejectsNegativeCurrentSavings() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setCurrentSavings(new BigDecimal("-1"));
        assertValidationMessage(request, "Current savings cannot be negative");
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

        FinanceProfileResponse response = service.saveForUser(userId, request);

        assertEquals(LocalDate.now(), response.getSetupDate());
    }

    @Test
    void saveRejectsUnparseableDate() {
        FinanceProfileRequest request = minimalRequest("1000");
        request.setSetupDate("01/02/2026");

        assertThrows(DateTimeParseException.class, () -> service.saveForUser(userId, request));
        verify(financeProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- saveForUser: JSON helpers

    @Test
    void investmentTypesAreJoinedFromArrays() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setInvestmentTypes(JSON.arrayNode().add("Stocks").add("").add("Gold"));

        assertEquals("Stocks, , Gold", service.saveForUser(userId, request).getInvestmentTypes());
    }

    @Test
    void investmentTypesIgnoreEmptyArraysNullAndBlankText() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        FinanceProfileRequest request = minimalRequest("1000");
        request.setInvestmentTypes(JSON.arrayNode().add(""));
        assertNull(service.saveForUser(userId, request).getInvestmentTypes());

        request.setInvestmentTypes(NullNode.getInstance());
        assertNull(service.saveForUser(userId, request).getInvestmentTypes());

        request.setInvestmentTypes(TextNode.valueOf("   "));
        assertNull(service.saveForUser(userId, request).getInvestmentTypes());

        request.setInvestmentTypes(TextNode.valueOf("  Gold  "));
        assertEquals("Gold", service.saveForUser(userId, request).getInvestmentTypes());
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
    void summaryIgnoresStoredExpensesAndEmi() {
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId))
                .thenReturn(Optional.of(profile("50000", "20000", "5000")));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(userId, summary.getUserId());
        assertEquals(new BigDecimal("50000"), summary.getMonthlyIncome());
        assertEquals(0, new BigDecimal("50000").compareTo(summary.getTotalMonthlyIncome()));
        assertEquals(0, new BigDecimal("50000").compareTo(summary.getBudgetCapacity()));
        assertEquals(0, new BigDecimal("50000").compareTo(summary.getRemainingBudgetCapacity()));
        assertEquals(0, new BigDecimal("50000").compareTo(summary.getGoalContributionCapacity()));
    }

    @Test
    void summarySubtractsCategoryBudgetsAndActiveGoalPlans() {
        FinanceProfile profile = profile("50000", "0", "0");
        profile.setAdditionalIncome(new BigDecimal("10000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        GoalResponse active = new GoalResponse();
        active.setStatus("active");
        active.setPlannedContribution(new BigDecimal("5000"));
        GoalResponse done = new GoalResponse();
        done.setStatus("completed");
        done.setPlannedContribution(new BigDecimal("9000"));
        when(goalService.list(userId)).thenReturn(List.of(active, done));
        BudgetResponse rent = new BudgetResponse();
        rent.setLimit(new BigDecimal("20000"));
        BudgetResponse food = new BudgetResponse();
        food.setLimit(new BigDecimal("10000"));
        when(budgetService.list(userId)).thenReturn(List.of(rent, food));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(0, new BigDecimal("60000").compareTo(summary.getTotalMonthlyIncome()));
        assertEquals(0, new BigDecimal("30000").compareTo(summary.getAllocatedBudgets()));
        assertEquals(0, new BigDecimal("5000").compareTo(summary.getPlannedGoalContributions()));
        assertEquals(0, new BigDecimal("55000").compareTo(summary.getBudgetCapacity()));
        assertEquals(0, new BigDecimal("25000").compareTo(summary.getRemainingBudgetCapacity()));
        assertEquals(0, new BigDecimal("25000").compareTo(summary.getGoalContributionCapacity()));
    }

    @Test
    void summaryExplainsMissingIncome() {
        FinanceProfile profile = new FinanceProfile();
        profile.setUserId(userId);
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(0, summary.getTotalMonthlyIncome().signum());
        assertEquals(1, summary.getInsights().size());
        assertTrue(summary.getInsights().get(0).contains("Add your monthly income"));
    }

    @Test
    void summaryCapsEmergencyReserveAtLiquidSavings() {
        FinanceProfile profile = profile("50000", "0", "0");
        profile.setCurrentSavings(new BigDecimal("1000"));
        profile.setEmergencyReserve(new BigDecimal("4000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));

        MonthlySummaryResponse summary = service.summary(userId);

        assertEquals(0, new BigDecimal("1000").compareTo(summary.getEmergencyReserve()));
        assertEquals(0, summary.getUnallocatedLiquid().signum());
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

    private FinanceProfileRequest minimalRequest(String income) {
        FinanceProfileRequest request = new FinanceProfileRequest();
        request.setMonthlyIncome(new BigDecimal(income));
        return request;
    }

    private FinanceProfileRequest fullRequest() {
        FinanceProfileRequest request = minimalRequest("50000");
        request.setIncomeSource("  Salary ");
        request.setPayCycle("Monthly");
        request.setAdditionalIncome(new BigDecimal("5000"));
        request.setCurrentSavings(new BigDecimal("15000"));
        request.setEmergencyReserve(new BigDecimal("6000"));
        request.setSavingsType("Bank");
        request.setInvestments(new BigDecimal("7000"));
        request.setInvestmentTypes(JSON.arrayNode().add("Stocks"));
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
