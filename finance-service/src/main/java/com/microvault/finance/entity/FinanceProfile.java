package com.microvault.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "finance_profiles")
public class FinanceProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "income_source")
    private String incomeSource;

    @Column(name = "monthly_income", nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "pay_cycle")
    private String payCycle;

    @Column(name = "monthly_expenses", nullable = false)
    private BigDecimal monthlyExpenses;

    @Column(name = "expense_categories")
    private String expenseCategories;

    @Column(name = "has_loan", nullable = false)
    private boolean hasLoan;

    @Column(name = "loan_type")
    private String loanType;

    @Column(name = "loan_amount", nullable = false)
    private BigDecimal loanAmount;

    @Column(name = "monthly_emi", nullable = false)
    private BigDecimal monthlyEmi;

    @Column(name = "emi_start_date")
    private LocalDate emiStartDate;

    @Column(name = "current_savings", nullable = false)
    private BigDecimal currentSavings;

    @Column(name = "savings_type")
    private String savingsType;

    @Column(name = "investments", nullable = false)
    private BigDecimal investments;

    @Column(name = "investment_types")
    private String investmentTypes;

    @Column(name = "monthly_budget", nullable = false)
    private BigDecimal monthlyBudget;

    @Column(name = "budget_style")
    private String budgetStyle;

    @Column(name = "setup_date")
    private LocalDate setupDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getIncomeSource() { return incomeSource; }
    public void setIncomeSource(String incomeSource) { this.incomeSource = incomeSource; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public String getPayCycle() { return payCycle; }
    public void setPayCycle(String payCycle) { this.payCycle = payCycle; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }
    public String getExpenseCategories() { return expenseCategories; }
    public void setExpenseCategories(String expenseCategories) { this.expenseCategories = expenseCategories; }
    public boolean isHasLoan() { return hasLoan; }
    public void setHasLoan(boolean hasLoan) { this.hasLoan = hasLoan; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public BigDecimal getMonthlyEmi() { return monthlyEmi; }
    public void setMonthlyEmi(BigDecimal monthlyEmi) { this.monthlyEmi = monthlyEmi; }
    public LocalDate getEmiStartDate() { return emiStartDate; }
    public void setEmiStartDate(LocalDate emiStartDate) { this.emiStartDate = emiStartDate; }
    public BigDecimal getCurrentSavings() { return currentSavings; }
    public void setCurrentSavings(BigDecimal currentSavings) { this.currentSavings = currentSavings; }
    public String getSavingsType() { return savingsType; }
    public void setSavingsType(String savingsType) { this.savingsType = savingsType; }
    public BigDecimal getInvestments() { return investments; }
    public void setInvestments(BigDecimal investments) { this.investments = investments; }
    public String getInvestmentTypes() { return investmentTypes; }
    public void setInvestmentTypes(String investmentTypes) { this.investmentTypes = investmentTypes; }
    public BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public String getBudgetStyle() { return budgetStyle; }
    public void setBudgetStyle(String budgetStyle) { this.budgetStyle = budgetStyle; }
    public LocalDate getSetupDate() { return setupDate; }
    public void setSetupDate(LocalDate setupDate) { this.setupDate = setupDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
