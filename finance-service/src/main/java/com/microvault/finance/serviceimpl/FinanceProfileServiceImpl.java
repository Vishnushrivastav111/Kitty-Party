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
import com.microvault.finance.service.FinancialPlan;
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
        BigDecimal takeHome = money(profile.getMonthlyIncome());
        BigDecimal additional = money(profile.getAdditionalIncome());
        BigDecimal income = FinancialPlan.totalMonthlyIncome(takeHome, additional);
        BigDecimal goalPlans = BigDecimal.ZERO;
        for (var goal : goalService.list(userId)) {
            if ("active".equalsIgnoreCase(goal.getStatus()) && goal.getPlannedContribution() != null) {
                goalPlans = goalPlans.add(goal.getPlannedContribution());
            }
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (var budget : budgetService.list(userId)) {
            if (budget.getLimit() != null) {
                allocated = allocated.add(budget.getLimit());
            }
        }
        BigDecimal budgetCapacity = FinancialPlan.budgetCapacity(income, goalPlans);
        BigDecimal remainingBudget = FinancialPlan.remainingBudgetCapacity(budgetCapacity, allocated);
        BigDecimal goalCapacity = FinancialPlan.goalContributionCapacity(income, allocated, goalPlans);
        BigDecimal emergency = money(profile.getEmergencyReserve());
        BigDecimal liquid = money(profile.getCurrentSavings());
        if (emergency.compareTo(liquid) > 0) {
            emergency = liquid;
        }
        MonthlySummaryResponse summary = new MonthlySummaryResponse();
        summary.setUserId(userId);
        summary.setMonthlyIncome(takeHome);
        summary.setTotalMonthlyIncome(income);
        summary.setAllocatedBudgets(FinancialPlan.money(allocated));
        summary.setPlannedGoalContributions(FinancialPlan.money(goalPlans));
        summary.setBudgetCapacity(budgetCapacity);
        summary.setRemainingBudgetCapacity(remainingBudget);
        summary.setGoalContributionCapacity(goalCapacity);
        summary.setEmergencyReserve(emergency);
        summary.setUnallocatedLiquid(liquid.subtract(emergency));
        summary.setInsights(insights(income, goalCapacity, goalPlans, allocated, remainingBudget, emergency));
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
        workspace.setSavingsOverview(savingsService.overview(userId));
        workspace.setBudgets(budgetService.list(userId));
        workspace.setReports(reportService.list(userId));
        workspace.setAffordChecks(affordabilityService.list(userId));
        workspace.setSetupSkipped(profile == null && setupSkipStore.isSkipped(userId));
        return workspace;
    }

    private List<String> insights(BigDecimal income, BigDecimal goalCapacity, BigDecimal goalPlans,
                                  BigDecimal allocated, BigDecimal remainingBudget, BigDecimal emergency) {
        List<String> notes = new ArrayList<>();
        if (income.signum() <= 0) {
            notes.add("Add your monthly income so budgets and goals can be checked against it.");
            return notes;
        }
        if (allocated.signum() > 0 && emergency.signum() > 0) {
            BigDecimal months = emergency.divide(allocated, 1, RoundingMode.HALF_UP);
            notes.add("Your emergency reserve covers about " + months.toPlainString()
                    + " months of your planned category budgets. Three to six months is a common guide, not a guarantee.");
        } else if (allocated.signum() > 0) {
            notes.add("No emergency reserve is set aside. Your category budgets total " + FinancialPlan.rupee(allocated) + " a month.");
        } else {
            notes.add("You have no category budgets yet. Create budgets for rent, food, EMI / Loans and other spending to see how much room remains.");
        }
        if (remainingBudget.signum() < 0) {
            notes.add("Category budgets and active goal contributions are above your monthly income by "
                    + FinancialPlan.rupee(remainingBudget.abs()) + ".");
        }
        if (goalPlans.signum() > 0 && FinancialPlan.nonNegative(goalCapacity).signum() == 0) {
            notes.add("Combined active goal contributions are above the monthly income left after category budgets.");
        } else if (FinancialPlan.nonNegative(goalCapacity).signum() > 0) {
            notes.add("About " + FinancialPlan.rupee(FinancialPlan.nonNegative(goalCapacity))
                    + " a month remains for new goal contributions after category budgets.");
        }
        return notes;
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
        if (request.getMonthlyIncome().signum() == 0) {
            throw new ValidationException("Monthly income must be greater than zero");
        }
        checkNotNegative(request.getAdditionalIncome(), "Additional income");
        checkNotNegative(request.getCurrentSavings(), "Current savings");
        checkNotNegative(request.getEmergencyReserve(), "Emergency reserve");
        checkNotNegative(request.getInvestments(), "Investments");
        BigDecimal savings = money(request.getCurrentSavings());
        BigDecimal emergency = money(request.getEmergencyReserve());
        if (emergency.compareTo(savings) > 0) {
            throw new ValidationException("Emergency reserve cannot be larger than current liquid savings");
        }
        LocalDate setupDate = parseDate(request.getSetupDate());
        if (setupDate != null && setupDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Setup date cannot be in the future");
        }
    }

    private void copy(FinanceProfileRequest request, FinanceProfile profile) {
        profile.setIncomeSource(clean(request.getIncomeSource()));
        profile.setMonthlyIncome(money(request.getMonthlyIncome()));
        profile.setAdditionalIncome(money(request.getAdditionalIncome()));
        profile.setPayCycle(clean(request.getPayCycle()));
        keepLegacyDefaults(profile);
        profile.setCurrentSavings(money(request.getCurrentSavings()));
        profile.setEmergencyReserve(money(request.getEmergencyReserve()));
        profile.setSavingsType(clean(request.getSavingsType()));
        profile.setInvestments(money(request.getInvestments()));
        profile.setInvestmentTypes(textList(request.getInvestmentTypes()));
        LocalDate setupDate = parseDate(request.getSetupDate());
        profile.setSetupDate(setupDate != null ? setupDate : LocalDate.now());
    }

    /**
     * Setup no longer collects expenses, loans, EMI, other commitments, or a monthly budget
     * (category budgets own that). The old columns stay in the table so no data is lost, are
     * never read by any calculation, and are only given zero defaults for brand-new rows.
     */
    private void keepLegacyDefaults(FinanceProfile profile) {
        if (profile.getMonthlyExpenses() == null) {
            profile.setMonthlyExpenses(BigDecimal.ZERO);
        }
        if (profile.getOtherCommitments() == null) {
            profile.setOtherCommitments(BigDecimal.ZERO);
        }
        if (profile.getLoanAmount() == null) {
            profile.setLoanAmount(BigDecimal.ZERO);
        }
        if (profile.getMonthlyEmi() == null) {
            profile.setMonthlyEmi(BigDecimal.ZERO);
        }
        if (profile.getMonthlyBudget() == null) {
            profile.setMonthlyBudget(BigDecimal.ZERO);
        }
    }

    private FinanceProfileResponse toResponse(FinanceProfile profile) {
        FinanceProfileResponse response = new FinanceProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setIncomeSource(profile.getIncomeSource());
        response.setMonthlyIncome(profile.getMonthlyIncome());
        response.setAdditionalIncome(profile.getAdditionalIncome());
        response.setOtherCommitments(profile.getOtherCommitments());
        response.setEmergencyReserve(profile.getEmergencyReserve());
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
