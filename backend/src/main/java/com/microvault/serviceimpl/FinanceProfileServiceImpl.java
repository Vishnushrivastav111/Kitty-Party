package com.microvault.serviceimpl;

import com.microvault.dao.FinanceProfileDAO;
import com.microvault.dto.FinanceProfileDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.FinanceProfile;
import com.microvault.service.FinanceProfileService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for financial profiles. The DAO is received through the
 * constructor, so this class is bound to the FinanceProfileDAO interface and
 * not to a concrete class.
 */
public class FinanceProfileServiceImpl implements FinanceProfileService {

    private final FinanceProfileDAO financeProfileDAO;

    public FinanceProfileServiceImpl(FinanceProfileDAO financeProfileDAO) {
        this.financeProfileDAO = financeProfileDAO;
    }

    @Override
    public FinanceProfileDTO createProfile(FinanceProfile financeProfile) {

        validateFinanceProfile(financeProfile);

        if (financeProfileDAO.findByUserId(financeProfile.getUserId()) != null) {
            throw new ValidationException("A financial profile already exists for this user");
        }

        applyDefaults(financeProfile);

        FinanceProfile savedProfile = financeProfileDAO.create(financeProfile);
        return toDTO(savedProfile);
    }

    @Override
    public FinanceProfileDTO getProfileById(UUID id) {
        if (id == null) {
            throw new ValidationException("Financial profile id is required");
        }
        FinanceProfile financeProfile = financeProfileDAO.findById(id);
        if (financeProfile == null) {
            throw new ValidationException("No active financial profile found with id " + id);
        }
        return toDTO(financeProfile);
    }

    @Override
    public FinanceProfileDTO getProfileByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        FinanceProfile financeProfile = financeProfileDAO.findByUserId(userId);
        if (financeProfile == null) {
            throw new ValidationException("No active financial profile found with id " + userId);
        }
        return toDTO(financeProfile);
    }

    @Override
    public List<FinanceProfileDTO> getAllProfiles() {
        return toDTOList(financeProfileDAO.findAll());
    }

    @Override
    public BigDecimal getMonthlySurplus(UUID userId) {

        if (userId == null) {
            throw new ValidationException("User id is required");
        }

        FinanceProfile financeProfile = financeProfileDAO.findByUserId(userId);
        if (financeProfile == null) {
            throw new ValidationException("No active financial profile found with id " + userId);
        }

        BigDecimal monthlyIncome = financeProfile.getMonthlyIncome() == null
                ? BigDecimal.ZERO : financeProfile.getMonthlyIncome();
        BigDecimal monthlyExpenses = financeProfile.getMonthlyExpenses() == null
                ? BigDecimal.ZERO : financeProfile.getMonthlyExpenses();

        BigDecimal monthlySurplus = monthlyIncome.subtract(monthlyExpenses);
        if (monthlySurplus.compareTo(BigDecimal.ZERO) < 0) {
            monthlySurplus = BigDecimal.ZERO;
        }

        return monthlySurplus;
    }

    @Override
    public boolean updateProfile(FinanceProfile financeProfile) {

        if (financeProfile == null || financeProfile.getId() == null) {
            throw new ValidationException("Financial profile id is required for an update");
        }
        validateFinanceProfile(financeProfile);

        FinanceProfile existingProfile = financeProfileDAO.findById(financeProfile.getId());
        if (existingProfile == null) {
            throw new ValidationException(
                    "No active financial profile found with id " + financeProfile.getId());
        }

        applyDefaults(financeProfile);

        return financeProfileDAO.update(financeProfile);
    }

    @Override
    public boolean softDeleteProfile(UUID id) {
        if (id == null) {
            throw new ValidationException("Financial profile id is required");
        }
        if (financeProfileDAO.findById(id) == null) {
            throw new ValidationException("No active financial profile found with id " + id);
        }
        return financeProfileDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateFinanceProfile(FinanceProfile financeProfile) {

        if (financeProfile == null) {
            throw new ValidationException("Financial profile is required");
        }
        if (financeProfile.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (financeProfile.getMonthlyIncome() == null) {
            throw new ValidationException("Monthly income is required");
        }
        if (financeProfile.getMonthlyIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Monthly income cannot be negative");
        }
        if (financeProfile.getMonthlyExpenses() != null
                && financeProfile.getMonthlyExpenses().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Monthly expenses cannot be negative");
        }
        if (financeProfile.getCurrentSavings() != null
                && financeProfile.getCurrentSavings().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Current savings cannot be negative");
        }
        if (financeProfile.getMonthlyBudget() != null
                && financeProfile.getMonthlyBudget().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Monthly budget cannot be negative");
        }
        if (financeProfile.getSetupDate() != null
                && financeProfile.getSetupDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Setup date cannot be in the future");
        }
    }

    /* ---------------- defaults ---------------- */

    /** The money columns are NOT NULL in the database, so empty means zero. */
    private void applyDefaults(FinanceProfile financeProfile) {

        if (financeProfile.getMonthlyIncome() == null) {
            financeProfile.setMonthlyIncome(BigDecimal.ZERO);
        }
        if (financeProfile.getMonthlyExpenses() == null) {
            financeProfile.setMonthlyExpenses(BigDecimal.ZERO);
        }
        if (financeProfile.getLoanAmount() == null) {
            financeProfile.setLoanAmount(BigDecimal.ZERO);
        }
        if (financeProfile.getMonthlyEmi() == null) {
            financeProfile.setMonthlyEmi(BigDecimal.ZERO);
        }
        if (financeProfile.getCurrentSavings() == null) {
            financeProfile.setCurrentSavings(BigDecimal.ZERO);
        }
        if (financeProfile.getInvestments() == null) {
            financeProfile.setInvestments(BigDecimal.ZERO);
        }
        if (financeProfile.getMonthlyBudget() == null) {
            financeProfile.setMonthlyBudget(BigDecimal.ZERO);
        }
        if (financeProfile.getHasLoan() == null) {
            financeProfile.setHasLoan(Boolean.FALSE);
        }
    }

    /* ---------------- model to DTO ---------------- */

    private FinanceProfileDTO toDTO(FinanceProfile financeProfile) {

        FinanceProfileDTO financeProfileDTO = new FinanceProfileDTO();
        financeProfileDTO.setId(financeProfile.getId());
        financeProfileDTO.setUserId(financeProfile.getUserId());
        financeProfileDTO.setIncomeSource(financeProfile.getIncomeSource());
        financeProfileDTO.setMonthlyIncome(financeProfile.getMonthlyIncome());
        financeProfileDTO.setPayCycle(financeProfile.getPayCycle());
        financeProfileDTO.setMonthlyExpenses(financeProfile.getMonthlyExpenses());
        financeProfileDTO.setExpenseCategories(financeProfile.getExpenseCategories());
        financeProfileDTO.setHasLoan(financeProfile.getHasLoan());
        financeProfileDTO.setLoanType(financeProfile.getLoanType());
        financeProfileDTO.setLoanAmount(financeProfile.getLoanAmount());
        financeProfileDTO.setMonthlyEmi(financeProfile.getMonthlyEmi());
        financeProfileDTO.setEmiStartDate(financeProfile.getEmiStartDate());
        financeProfileDTO.setCurrentSavings(financeProfile.getCurrentSavings());
        financeProfileDTO.setSavingsType(financeProfile.getSavingsType());
        financeProfileDTO.setInvestments(financeProfile.getInvestments());
        financeProfileDTO.setInvestmentTypes(financeProfile.getInvestmentTypes());
        financeProfileDTO.setMonthlyBudget(financeProfile.getMonthlyBudget());
        financeProfileDTO.setBudgetStyle(financeProfile.getBudgetStyle());
        financeProfileDTO.setSetupDate(financeProfile.getSetupDate());
        financeProfileDTO.setCreatedAt(financeProfile.getCreatedAt());
        return financeProfileDTO;
    }

    private List<FinanceProfileDTO> toDTOList(List<FinanceProfile> financeProfiles) {
        List<FinanceProfileDTO> financeProfileDTOs = new ArrayList<>();
        for (FinanceProfile financeProfile : financeProfiles) {
            financeProfileDTOs.add(toDTO(financeProfile));
        }
        return financeProfileDTOs;
    }
}
