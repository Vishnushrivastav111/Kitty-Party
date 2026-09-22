package com.microvault.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents one row of the "finance_profiles" table. A member fills this in
 * once during setup, so there is one active profile per user.
 */
public class FinanceProfile {

    private UUID id;
    private UUID userId;
    private String incomeSource;
    private BigDecimal monthlyIncome;
    private String payCycle;
    private BigDecimal monthlyExpenses;
    private String expenseCategories;
    private Boolean hasLoan;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal monthlyEmi;
    private LocalDate emiStartDate;
    private BigDecimal currentSavings;
    private String savingsType;
    private BigDecimal investments;
    private String investmentTypes;
    private BigDecimal monthlyBudget;
    private String budgetStyle;
    private LocalDate setupDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    public FinanceProfile() {
    }

    public FinanceProfile(UUID userId, BigDecimal monthlyIncome, BigDecimal monthlyExpenses,
                          BigDecimal currentSavings, BigDecimal monthlyBudget, LocalDate setupDate) {
        this.userId = userId;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.currentSavings = currentSavings;
        this.monthlyBudget = monthlyBudget;
        this.setupDate = setupDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getIncomeSource() {
        return incomeSource;
    }

    public void setIncomeSource(String incomeSource) {
        this.incomeSource = incomeSource;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public String getPayCycle() {
        return payCycle;
    }

    public void setPayCycle(String payCycle) {
        this.payCycle = payCycle;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public String getExpenseCategories() {
        return expenseCategories;
    }

    public void setExpenseCategories(String expenseCategories) {
        this.expenseCategories = expenseCategories;
    }

    public Boolean getHasLoan() {
        return hasLoan;
    }

    public void setHasLoan(Boolean hasLoan) {
        this.hasLoan = hasLoan;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyEmi;
    }

    public void setMonthlyEmi(BigDecimal monthlyEmi) {
        this.monthlyEmi = monthlyEmi;
    }

    public LocalDate getEmiStartDate() {
        return emiStartDate;
    }

    public void setEmiStartDate(LocalDate emiStartDate) {
        this.emiStartDate = emiStartDate;
    }

    public BigDecimal getCurrentSavings() {
        return currentSavings;
    }

    public void setCurrentSavings(BigDecimal currentSavings) {
        this.currentSavings = currentSavings;
    }

    public String getSavingsType() {
        return savingsType;
    }

    public void setSavingsType(String savingsType) {
        this.savingsType = savingsType;
    }

    public BigDecimal getInvestments() {
        return investments;
    }

    public void setInvestments(BigDecimal investments) {
        this.investments = investments;
    }

    public String getInvestmentTypes() {
        return investmentTypes;
    }

    public void setInvestmentTypes(String investmentTypes) {
        this.investmentTypes = investmentTypes;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public String getBudgetStyle() {
        return budgetStyle;
    }

    public void setBudgetStyle(String budgetStyle) {
        this.budgetStyle = budgetStyle;
    }

    public LocalDate getSetupDate() {
        return setupDate;
    }

    public void setSetupDate(LocalDate setupDate) {
        this.setupDate = setupDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "FinanceProfile{id=" + id
                + ", userId=" + userId
                + ", monthlyIncome=" + monthlyIncome
                + ", monthlyExpenses=" + monthlyExpenses
                + ", currentSavings=" + currentSavings
                + ", monthlyBudget=" + monthlyBudget
                + ", setupDate=" + setupDate
                + ", isDeleted=" + isDeleted
                + '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FinanceProfile)) {
            return false;
        }
        FinanceProfile financeProfile = (FinanceProfile) other;
        return Objects.equals(id, financeProfile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
