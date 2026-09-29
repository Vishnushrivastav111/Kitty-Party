package com.microvault.financeprofile.serviceimpl;

import com.microvault.financeprofile.dto.FinanceProfileRequest;
import com.microvault.financeprofile.dto.FinanceProfileResponse;
import com.microvault.financeprofile.dto.MonthlySummaryResponse;
import com.microvault.financeprofile.dto.SurplusResponse;
import com.microvault.financeprofile.exception.DuplicateProfileException;
import com.microvault.financeprofile.exception.ProfileNotFoundException;
import com.microvault.financeprofile.exception.ValidationException;
import com.microvault.financeprofile.model.FinanceProfile;
import com.microvault.financeprofile.repository.FinanceProfileRepository;
import com.microvault.financeprofile.service.FinanceProfileService;
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
@Transactional(readOnly = true)
public class FinanceProfileServiceImpl implements FinanceProfileService {

    private final FinanceProfileRepository financeProfileRepository;

    public FinanceProfileServiceImpl(FinanceProfileRepository financeProfileRepository) {
        this.financeProfileRepository = financeProfileRepository;
    }

    @Override
    @Transactional
    public FinanceProfileResponse createProfile(FinanceProfileRequest request) {
        validateRequest(request, true);

        if (financeProfileRepository.findByUserIdAndIsDeletedFalse(request.getUserId()).isPresent()) {
            throw new DuplicateProfileException("A financial profile already exists for this user");
        }

        FinanceProfile profile = new FinanceProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(request.getUserId());
        copyFields(request, profile);

        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setIsDeleted(Boolean.FALSE);
        profile.setDeletedAt(null);

        return toResponse(financeProfileRepository.save(profile));
    }

    @Override
    public FinanceProfileResponse getProfileById(UUID id) {
        return toResponse(loadById(id));
    }

    @Override
    public FinanceProfileResponse getProfileByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toResponse(loadByUserId(userId));
    }

    @Override
    public List<FinanceProfileResponse> getAllProfiles() {
        List<FinanceProfileResponse> responses = new ArrayList<>();
        for (FinanceProfile profile : financeProfileRepository.findByIsDeletedFalseOrderByCreatedAtDesc()) {
            responses.add(toResponse(profile));
        }
        return responses;
    }

    @Override
    public SurplusResponse getMonthlySurplus(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        FinanceProfile profile = loadByUserId(userId);
        return new SurplusResponse(userId, surplusAfterExpenses(profile));
    }

    @Override
    public MonthlySummaryResponse getMonthlySummary(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }

        FinanceProfile profile = loadByUserId(userId);

        BigDecimal income = money(profile.getMonthlyIncome());
        BigDecimal expenses = money(profile.getMonthlyExpenses());
        BigDecimal emi = money(profile.getMonthlyEmi());
        BigDecimal surplus = surplusAfterExpenses(profile);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = surplus
                    .multiply(new BigDecimal("100"))
                    .divide(income, 2, RoundingMode.HALF_UP);
        }

        MonthlySummaryResponse summary = new MonthlySummaryResponse();
        summary.setProfileId(profile.getId());
        summary.setUserId(profile.getUserId());
        summary.setMonthlyIncome(income);
        summary.setMonthlyExpenses(expenses);
        summary.setMonthlyEmi(emi);
        summary.setMonthlyBudget(money(profile.getMonthlyBudget()));
        summary.setCurrentSavings(money(profile.getCurrentSavings()));
        summary.setInvestments(money(profile.getInvestments()));
        summary.setMonthlySurplus(surplus);
        summary.setLeftAfterEmi(income.subtract(expenses).subtract(emi));
        summary.setSavingsRatePercent(savingsRate);
        return summary;
    }

    @Override
    @Transactional
    public FinanceProfileResponse updateProfile(UUID id, FinanceProfileRequest request) {
        if (id == null) {
            throw new ValidationException("Financial profile id is required");
        }
        validateRequest(request, false);

        FinanceProfile profile = loadById(id);
        if (request.getUserId() != null && !request.getUserId().equals(profile.getUserId())) {
            throw new ValidationException("User id cannot be changed");
        }

        copyFields(request, profile);
        profile.setUpdatedAt(LocalDateTime.now());
        return toResponse(financeProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public void softDeleteProfile(UUID id) {
        FinanceProfile profile = loadById(id);
        LocalDateTime now = LocalDateTime.now();
        profile.setIsDeleted(Boolean.TRUE);
        profile.setDeletedAt(now);
        profile.setUpdatedAt(now);
        financeProfileRepository.save(profile);
    }

    private FinanceProfile loadById(UUID id) {
        if (id == null) {
            throw new ValidationException("Financial profile id is required");
        }
        return financeProfileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "No active financial profile found with id " + id));
    }

    private FinanceProfile loadByUserId(UUID userId) {
        return financeProfileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "No active financial profile found for user " + userId));
    }

    /**
     * Income minus expenses. A negative result is treated as zero so callers
     * can use the number as "money available" without an extra check.
     */
    private BigDecimal surplusAfterExpenses(FinanceProfile profile) {
        BigDecimal surplus = money(profile.getMonthlyIncome()).subtract(money(profile.getMonthlyExpenses()));
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return surplus;
    }

    private void validateRequest(FinanceProfileRequest request, boolean userIdRequired) {
        if (request == null) {
            throw new ValidationException("Financial profile is required");
        }
        if (userIdRequired && request.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (request.getMonthlyIncome() == null) {
            throw new ValidationException("Monthly income is required");
        }

        checkNotNegative(request.getMonthlyIncome(), "Monthly income");
        checkNotNegative(request.getMonthlyExpenses(), "Monthly expenses");
        checkNotNegative(request.getCurrentSavings(), "Current savings");
        checkNotNegative(request.getMonthlyBudget(), "Monthly budget");
        checkNotNegative(request.getLoanAmount(), "Loan amount");
        checkNotNegative(request.getMonthlyEmi(), "Monthly EMI");
        checkNotNegative(request.getInvestments(), "Investments");

        if (request.getSetupDate() != null && request.getSetupDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Setup date cannot be in the future");
        }

        checkLength(request.getIncomeSource(), 50, "Income source");
        checkLength(request.getPayCycle(), 30, "Pay cycle");
        checkLength(request.getLoanType(), 50, "Loan type");
        checkLength(request.getSavingsType(), 50, "Savings type");
        checkLength(request.getBudgetStyle(), 50, "Budget style");
    }

    private void checkNotNegative(BigDecimal amount, String label) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(label + " cannot be negative");
        }
    }

    private void checkLength(String value, int max, String label) {
        if (value != null && value.trim().length() > max) {
            throw new ValidationException(label + " cannot be longer than " + max + " characters");
        }
    }

    /** Money columns are NOT NULL, so a missing amount is stored as zero. */
    private void copyFields(FinanceProfileRequest request, FinanceProfile profile) {
        profile.setIncomeSource(clean(request.getIncomeSource()));
        profile.setMonthlyIncome(money(request.getMonthlyIncome()));
        profile.setPayCycle(clean(request.getPayCycle()));
        profile.setMonthlyExpenses(money(request.getMonthlyExpenses()));
        profile.setExpenseCategories(clean(request.getExpenseCategories()));
        profile.setHasLoan(Boolean.TRUE.equals(request.getHasLoan()));
        profile.setLoanType(clean(request.getLoanType()));
        profile.setLoanAmount(money(request.getLoanAmount()));
        profile.setMonthlyEmi(money(request.getMonthlyEmi()));
        profile.setEmiStartDate(request.getEmiStartDate());
        profile.setCurrentSavings(money(request.getCurrentSavings()));
        profile.setSavingsType(clean(request.getSavingsType()));
        profile.setInvestments(money(request.getInvestments()));
        profile.setInvestmentTypes(clean(request.getInvestmentTypes()));
        profile.setMonthlyBudget(money(request.getMonthlyBudget()));
        profile.setBudgetStyle(clean(request.getBudgetStyle()));
        profile.setSetupDate(request.getSetupDate());
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount;
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
        response.setHasLoan(profile.getHasLoan());
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
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
