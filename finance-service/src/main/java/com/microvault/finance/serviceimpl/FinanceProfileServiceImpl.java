package com.microvault.finance.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.service.BudgetService;
import com.microvault.finance.service.FinanceProfileService;
import com.microvault.finance.service.GoalService;
import com.microvault.finance.service.ReportService;
import com.microvault.finance.service.SavingsService;
import com.microvault.finance.service.SetupSkipStore;
import com.microvault.finance.service.TransactionService;
import com.microvault.finance.service.AffordabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FinanceProfileServiceImpl implements FinanceProfileService {

    private final FinanceProfileRepository financeProfileRepository;
    private final SetupSkipStore setupSkipStore;
    private final TransactionService transactionService;
    private final GoalService goalService;
    private final SavingsService savingsService;
    private final BudgetService budgetService;
    private final ReportService reportService;
    private final AffordabilityService affordabilityService;

    public FinanceProfileServiceImpl(FinanceProfileRepository financeProfileRepository,
                                     SetupSkipStore setupSkipStore,
                                     TransactionService transactionService,
                                     GoalService goalService,
                                     SavingsService savingsService,
                                     BudgetService budgetService,
                                     ReportService reportService,
                                     AffordabilityService affordabilityService) {
        this.financeProfileRepository = financeProfileRepository;
        this.setupSkipStore = setupSkipStore;
        this.transactionService = transactionService;
        this.goalService = goalService;
        this.savingsService = savingsService;
        this.budgetService = budgetService;
        this.reportService = reportService;
        this.affordabilityService = affordabilityService;
    }

    @Override
    public FinanceProfileResponse saveForUser(UUID userId, FinanceProfileRequest request) {
        validate(request);
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (profile == null) {
            profile = new FinanceProfile();
            profile.setId(UUID.randomUUID());
            profile.setUserId(userId);
            profile.setCreatedAt(now);
            profile.setDeleted(false);
        }
        copy(request, profile);
        profile.setUpdatedAt(now);
        setupSkipStore.clear(userId);
        return toResponse(financeProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceProfileResponse getByUserId(UUID userId) {
        return toResponse(load(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse summary(UUID userId) {
        FinanceProfile profile = load(userId);
        BigDecimal income = money(profile.getMonthlyIncome());
        BigDecimal expenses = money(profile.getMonthlyExpenses());
        BigDecimal surplus = income.subtract(expenses);
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            surplus = BigDecimal.ZERO;
        }
        BigDecimal rate = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            rate = surplus.multiply(new BigDecimal("100")).divide(income, 2, RoundingMode.HALF_UP);
        }
        MonthlySummaryResponse summary = new MonthlySummaryResponse();
        summary.setUserId(userId);
        summary.setMonthlyIncome(income);
        summary.setMonthlyExpenses(expenses);
        summary.setMonthlyEmi(money(profile.getMonthlyEmi()));
        summary.setMonthlySurplus(surplus);
        summary.setSavingsRatePercent(rate);
        return summary;
    }

    @Override
    public void skipSetup(UUID userId) {
        setupSkipStore.skip(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> activeUserIds() {
        List<UUID> ids = new ArrayList<>();
        for (FinanceProfile profile : financeProfileRepository.findByDeletedFalse()) {
            ids.add(profile.getUserId());
        }
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceWorkspaceResponse workspace(UUID userId) {
        FinanceWorkspaceResponse workspace = new FinanceWorkspaceResponse();
        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        if (profile != null) {
            workspace.setFinance(toResponse(profile));
        }
        workspace.setTransactions(transactionService.list(userId));
        workspace.setGoals(goalService.list(userId));
        workspace.setSavings(savingsService.list(userId));
        workspace.setBudgets(budgetService.list(userId));
        workspace.setReports(reportService.list(userId));
        workspace.setAffordChecks(affordabilityService.list(userId));
        workspace.setSetupSkipped(profile == null && setupSkipStore.isSkipped(userId));
        return workspace;
    }

    private FinanceProfile load(UUID userId) {
        return financeProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active financial profile found for this user"));
    }

    private void validate(FinanceProfileRequest request) {
        if (request == null || request.getMonthlyIncome() == null) {
            throw new ValidationException("Monthly income is required");
        }
        checkNotNegative(request.getMonthlyIncome(), "Monthly income");
        checkNotNegative(request.getMonthlyExpenses(), "Monthly expenses");
        checkNotNegative(request.getCurrentSavings(), "Current savings");
        checkNotNegative(request.getMonthlyBudget(), "Monthly budget");
        checkNotNegative(request.getLoanAmount(), "Loan amount");
        checkNotNegative(emi(request), "Monthly EMI");
        checkNotNegative(request.getInvestments(), "Investments");
        LocalDate setupDate = parseDate(request.getSetupDate());
        if (setupDate != null && setupDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Setup date cannot be in the future");
        }
    }

    private void copy(FinanceProfileRequest request, FinanceProfile profile) {
        profile.setIncomeSource(clean(request.getIncomeSource()));
        profile.setMonthlyIncome(money(request.getMonthlyIncome()));
        profile.setPayCycle(clean(request.getPayCycle()));
        profile.setMonthlyExpenses(money(request.getMonthlyExpenses()));
        profile.setExpenseCategories(textList(request.getExpenseCategories()));
        profile.setHasLoan(yes(request.getHasLoan()));
        profile.setLoanType(clean(request.getLoanType()));
        profile.setLoanAmount(money(request.getLoanAmount()));
        profile.setMonthlyEmi(money(emi(request)));
        profile.setEmiStartDate(parseDate(request.getEmiStartDate()));
        profile.setCurrentSavings(money(request.getCurrentSavings()));
        profile.setSavingsType(clean(request.getSavingsType()));
        profile.setInvestments(money(request.getInvestments()));
        profile.setInvestmentTypes(textList(request.getInvestmentTypes()));
        profile.setMonthlyBudget(money(request.getMonthlyBudget()));
        profile.setBudgetStyle(clean(request.getBudgetStyle()));
        profile.setSetupDate(parseDate(request.getSetupDate()));
    }

    private FinanceProfileResponse toResponse(FinanceProfile profile) {
        FinanceProfileResponse response = new FinanceProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setIncomeSource(profile.getIncomeSource());
        response.setMonthlyIncome(profile.getMonthlyIncome());
        response.setPayCycle(profile.getPayCycle());
        response.setMonthlyExpenses(profile.getMonthlyExpenses());
        response.setExpenseCategories(profile.getExpenseCategories());
        response.setHasLoan(profile.isHasLoan());
        response.setLoanType(profile.getLoanType());
        response.setLoanAmount(profile.getLoanAmount());
        response.setMonthlyEmi(profile.getMonthlyEmi());
        response.setEmiStartDate(profile.getEmiStartDate());
        response.setCurrentSavings(profile.getCurrentSavings());
        response.setSavingsType(profile.getSavingsType());
        response.setInvestments(profile.getInvestments());
        response.setInvestmentTypes(profile.getInvestmentTypes());
        response.setMonthlyBudget(profile.getMonthlyBudget());
        response.setBudgetStyle(profile.getBudgetStyle());
        response.setSetupDate(profile.getSetupDate());
        return response;
    }

    private BigDecimal emi(FinanceProfileRequest request) {
        if (request.getMonthlyEmi() != null) {
            return request.getMonthlyEmi();
        }
        return request.getLoansEmi();
    }

    private boolean yes(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        String text = node.asText("");
        return "yes".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text);
    }

    private String textList(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : node) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(item.asText());
            }
            String text = builder.toString().trim();
            return text.isEmpty() ? null : text;
        }
        return clean(node.asText());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private void checkNotNegative(BigDecimal amount, String label) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(label + " cannot be negative");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
