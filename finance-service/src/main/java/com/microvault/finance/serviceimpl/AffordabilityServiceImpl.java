package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.Budget;
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
import com.microvault.finance.service.AffordabilityService;
import com.microvault.finance.service.FinancialPlan;
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
public class AffordabilityServiceImpl implements AffordabilityService {

    private final AffordabilityRepository affordabilityRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final SavingsRepository savingsRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;

    public AffordabilityServiceImpl(AffordabilityRepository affordabilityRepository,
                                    FinanceProfileRepository financeProfileRepository,
                                    SavingsRepository savingsRepository,
                                    BudgetRepository budgetRepository,
                                    GoalRepository goalRepository) {
        this.affordabilityRepository = affordabilityRepository;
        this.financeProfileRepository = financeProfileRepository;
        this.savingsRepository = savingsRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    public AffordabilityResponse check(UUID userId, AffordabilityRequest request) {
        if (request.getCheckDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Check date cannot be in the future");
        }
        String payment = request.getPaymentMethod() == null ? "cash" : request.getPaymentMethod().trim().toLowerCase();
        if (!"cash".equals(payment) && !"emi".equals(payment)) {
            throw new ValidationException("Payment method must be cash or emi");
        }
        BigDecimal installment = null;
        BigDecimal totalRepayment = null;
        if ("emi".equals(payment)) {
            if (request.getEmiMonths() == null || request.getEmiMonths() < 1) {
                throw new ValidationException("Enter the number of EMI months");
            }
            installment = FinancialPlan.monthlyInstallment(request.getAmount(), request.getEmiMonths(), request.getAnnualInterestPercent());
            if (installment == null) {
                throw new ValidationException("EMI details are not enough to calculate a repayment");
            }
            totalRepayment = installment.multiply(BigDecimal.valueOf(request.getEmiMonths())).setScale(2, RoundingMode.HALF_UP);
        }

        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        List<SavingsEntry> savings = savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        BigDecimal spendable = spendableCash(profile, savings);
        BigDecimal planning = planning(profile);
        BigDecimal room = monthlyRoom(userId, profile, planning);

        String verdict;
        String level;
        String suggestion;
        BigDecimal shortfall = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Integer monthsToSave = null;
        boolean canCreateGoal = false;

        if (profile == null) {
            verdict = "Not currently affordable";
            level = "danger";
            suggestion = "Complete financial setup before treating this purchase as affordable. Spendable cash on record is "
                    + FinancialPlan.rupee(spendable) + ".";
        } else if (planning.signum() < 0) {
            verdict = "Not currently affordable";
            level = "danger";
            suggestion = "Essential expenses, EMI, and other commitments already exceed monthly income by "
                    + FinancialPlan.rupee(planning.abs()) + ". This purchase would add to that gap.";
        } else if ("emi".equals(payment)) {
            if (installment.compareTo(FinancialPlan.nonNegative(room)) <= 0 && room.signum() > 0) {
                verdict = "Affordable now";
                level = "success";
                suggestion = "The extra EMI of " + FinancialPlan.rupee(installment)
                        + " fits the " + FinancialPlan.rupee(room)
                        + " still available each month. Total repayment is " + FinancialPlan.rupee(totalRepayment)
                        + ". This is planning guidance, not a guarantee.";
            } else {
                verdict = "Not currently affordable";
                level = "danger";
                suggestion = "The extra EMI of " + FinancialPlan.rupee(installment)
                        + " is above the " + FinancialPlan.rupee(FinancialPlan.nonNegative(room))
                        + " left after obligations, budgets, and active goals. A longer tenure or a smaller amount may fit.";
            }
        } else if (request.getAmount().compareTo(spendable) <= 0) {
            verdict = "Affordable now";
            level = "success";
            suggestion = "Unallocated cash of " + FinancialPlan.rupee(spendable)
                    + " covers " + FinancialPlan.rupee(request.getAmount())
                    + " without using emergency reserves, goal savings, or investments.";
        } else {
            shortfall = FinancialPlan.money(request.getAmount().subtract(spendable));
            BigDecimal capacity = FinancialPlan.nonNegative(room);
            if (capacity.signum() > 0) {
                monthsToSave = shortfall.divide(capacity, 0, RoundingMode.CEILING).intValue();
                if (monthsToSave < 1) {
                    monthsToSave = 1;
                }
                verdict = "Affordable with planning";
                level = "warning";
                canCreateGoal = true;
                suggestion = "You would need another " + FinancialPlan.rupee(shortfall)
                        + ". About " + monthsToSave + " month" + (monthsToSave == 1 ? "" : "s")
                        + " at " + FinancialPlan.rupee(capacity) + " a month can close that gap. Create a savings goal if you want to track it.";
            } else {
                verdict = "Not currently affordable";
                level = "danger";
                suggestion = "The shortfall is " + FinancialPlan.rupee(shortfall)
                        + " and there is no monthly capacity left for a new savings plan after obligations, budgets, and active goals.";
            }
        }

        AffordabilityCheck check = new AffordabilityCheck();
        check.setId(UUID.randomUUID());
        check.setUserId(userId);
        check.setItemName(request.getItemName().trim());
        check.setAmount(request.getAmount());
        check.setAvailableAmount(spendable);
        check.setVerdict(verdict);
        check.setLevel(level);
        check.setPriority(request.getPriority() == null ? "Want" : request.getPriority());
        check.setPaymentMethod(payment);
        check.setEmiMonths(request.getEmiMonths());
        check.setMonthlyInstallment(installment);
        check.setTotalRepayment(totalRepayment);
        check.setCheckDate(request.getCheckDate());
        LocalDateTime now = LocalDateTime.now();
        check.setCreatedAt(now);
        check.setUpdatedAt(now);
        check.setDeleted(false);

        AffordabilityResponse response = toResponse(affordabilityRepository.save(check));
        response.setSuggestion(suggestion);
        response.setShortfall(shortfall);
        response.setMonthsToSave(monthsToSave);
        response.setMonthlyInstallment(installment);
        response.setTotalRepayment(totalRepayment);
        response.setPaymentMethod(payment);
        response.setCanCreateGoal(canCreateGoal);
        response.setPlan(buildPlan(profile, spendable, planning, room, request.getAmount(), installment, totalRepayment, suggestion));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffordabilityResponse> list(UUID userId) {
        List<AffordabilityResponse> rows = new ArrayList<>();
        for (AffordabilityCheck check : affordabilityRepository
                .findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId)) {
            rows.add(toResponse(check));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        AffordabilityCheck check = affordabilityRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Affordability check not found"));
        LocalDateTime now = LocalDateTime.now();
        check.setDeleted(true);
        check.setDeletedAt(now);
        check.setUpdatedAt(now);
        affordabilityRepository.save(check);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        for (AffordabilityCheck check : affordabilityRepository
                .findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId)) {
            check.setDeleted(true);
            check.setDeletedAt(now);
            check.setUpdatedAt(now);
            affordabilityRepository.save(check);
        }
    }

    private BigDecimal planning(FinanceProfile profile) {
        if (profile == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal income = FinancialPlan.totalMonthlyIncome(profile.getMonthlyIncome(), profile.getAdditionalIncome());
        return FinancialPlan.planningCapacity(income, profile.getMonthlyExpenses(), profile.getMonthlyEmi(), profile.getOtherCommitments());
    }

    private BigDecimal monthlyRoom(UUID userId, FinanceProfile profile, BigDecimal planning) {
        if (profile == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal budgets = BigDecimal.ZERO;
        for (Budget budget : budgetRepository.findByUserIdAndDeletedFalseOrderByCategoryAsc(userId)) {
            if (budget.getLimit() != null) {
                budgets = budgets.add(budget.getLimit());
            }
        }
        BigDecimal goals = BigDecimal.ZERO;
        for (Goal goal : goalRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            if ("active".equalsIgnoreCase(goal.getStatus()) && goal.getPlannedContribution() != null) {
                goals = goals.add(goal.getPlannedContribution());
            }
        }
        return FinancialPlan.goalContributionCapacity(planning, budgets, goals);
    }

    /**
     * Spendable cash is unallocated liquid savings plus later general contributions.
     * Emergency reserves, goal-linked contributions, and investments are not spendable.
     * An "Opening" entry is ignored when current savings already records that balance.
     */
    private BigDecimal spendableCash(FinanceProfile profile, List<SavingsEntry> entries) {
        BigDecimal current = profile == null ? BigDecimal.ZERO : FinancialPlan.money(profile.getCurrentSavings());
        BigDecimal emergency = profile == null ? BigDecimal.ZERO : FinancialPlan.money(profile.getEmergencyReserve());
        if (emergency.compareTo(current) > 0) {
            emergency = current;
        }
        BigDecimal total = current.subtract(emergency);
        for (SavingsEntry entry : entries) {
            if (entry.getAmount() == null || entry.getGoalId() != null) {
                continue;
            }
            if (FinancialPlan.emergencyCategory(entry.getCategory())) {
                continue;
            }
            if (current.signum() > 0 && FinancialPlan.openingMirror(entry.getCategory())) {
                continue;
            }
            total = total.add(entry.getAmount());
        }
        return FinancialPlan.money(total);
    }

    private List<String> buildPlan(FinanceProfile profile, BigDecimal spendable, BigDecimal planning, BigDecimal room,
                                   BigDecimal amount, BigDecimal installment, BigDecimal totalRepayment, String suggestion) {
        List<String> plan = new ArrayList<>();
        BigDecimal income = profile == null ? FinancialPlan.money(null)
                : FinancialPlan.totalMonthlyIncome(profile.getMonthlyIncome(), profile.getAdditionalIncome());
        BigDecimal expenses = profile == null ? FinancialPlan.money(null) : FinancialPlan.money(profile.getMonthlyExpenses());
        BigDecimal emi = profile == null ? FinancialPlan.money(null) : FinancialPlan.money(profile.getMonthlyEmi());
        plan.add("Monthly planning capacity starts from income " + income + " minus essential expenses " + expenses
                + ", EMI " + emi + ", and other commitments. Result: " + planning + ".");
        plan.add("Spendable cash excludes emergency reserves, goal-linked savings, and investments. It is " + spendable + ".");
        plan.add("Monthly room left for a new commitment after budgets and active goals is " + room + ".");
        plan.add("The purchase amount is " + amount + ".");
        if (installment != null) {
            plan.add("Financed repayment is " + installment + " a month, " + totalRepayment + " in total.");
        }
        plan.add(suggestion);
        return plan;
    }

    private AffordabilityResponse toResponse(AffordabilityCheck check) {
        AffordabilityResponse response = new AffordabilityResponse();
        response.setId(check.getId());
        response.setItemName(check.getItemName());
        response.setAmount(check.getAmount());
        response.setAvailable(check.getAvailableAmount());
        response.setAvailableAmount(check.getAvailableAmount());
        response.setVerdict(check.getVerdict());
        response.setLevel(check.getLevel());
        response.setPriority(check.getPriority());
        response.setDate(check.getCheckDate());
        response.setPaymentMethod(check.getPaymentMethod());
        response.setMonthlyInstallment(check.getMonthlyInstallment());
        response.setTotalRepayment(check.getTotalRepayment());
        return response;
    }
}
