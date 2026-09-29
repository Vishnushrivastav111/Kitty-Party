package com.microvault.financeprofile.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of finance_profiles. A member has one active profile.
 */
@Entity
@Table(name = "finance_profiles")
public class FinanceProfile {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "income_source", length = 50)
    private String incomeSource;

    @Column(name = "monthly_income", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "pay_cycle", length = 30)
    private String payCycle;

    @Column(name = "monthly_expenses", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyExpenses;

    @Column(name = "expense_categories")
    private String expenseCategories;

    @Column(name = "has_loan", nullable = false)
    private Boolean hasLoan;

    @Column(name = "loan_type", length = 50)
    private String loanType;

    @Column(name = "loan_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "monthly_emi", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyEmi;

    @Column(name = "emi_start_date")
    private LocalDate emiStartDate;

    @Column(name = "current_savings", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentSavings;

    @Column(name = "savings_type", length = 50)
    private String savingsType;

    @Column(name = "investments", nullable = false, precision = 14, scale = 2)
    private BigDecimal investments;

    @Column(name = "investment_types")
    private String investmentTypes;

    @Column(name = "monthly_budget", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "budget_style", length = 50)
    private String budgetStyle;

    @Column(name = "setup_date")
    private LocalDate setupDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
}
