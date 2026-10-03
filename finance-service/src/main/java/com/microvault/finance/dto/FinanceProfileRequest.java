package com.microvault.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FinanceProfileRequest {

    private String incomeSource;
    private BigDecimal monthlyIncome;
    private String payCycle;
    private BigDecimal monthlyExpenses;
    private JsonNode expenseCategories;
    private JsonNode hasLoan;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal monthlyEmi;
    private BigDecimal loansEmi;
    private String emiStartDate;
    private BigDecimal currentSavings;
    private String savingsType;
    private BigDecimal investments;
    private JsonNode investmentTypes;
    private BigDecimal monthlyBudget;
    private String budgetStyle;
    private String setupDate;

    public String getIncomeSource() { return incomeSource; }
    public void setIncomeSource(String incomeSource) { this.incomeSource = incomeSource; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public String getPayCycle() { return payCycle; }
    public void setPayCycle(String payCycle) { this.payCycle = payCycle; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }
    public JsonNode getExpenseCategories() { return expenseCategories; }
    public void setExpenseCategories(JsonNode expenseCategories) { this.expenseCategories = expenseCategories; }
    public JsonNode getHasLoan() { return hasLoan; }
    public void setHasLoan(JsonNode hasLoan) { this.hasLoan = hasLoan; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public BigDecimal getMonthlyEmi() { return monthlyEmi; }
    public void setMonthlyEmi(BigDecimal monthlyEmi) { this.monthlyEmi = monthlyEmi; }
    public BigDecimal getLoansEmi() { return loansEmi; }
    public void setLoansEmi(BigDecimal loansEmi) { this.loansEmi = loansEmi; }
    public String getEmiStartDate() { return emiStartDate; }
    public void setEmiStartDate(String emiStartDate) { this.emiStartDate = emiStartDate; }
    public BigDecimal getCurrentSavings() { return currentSavings; }
    public void setCurrentSavings(BigDecimal currentSavings) { this.currentSavings = currentSavings; }
    public String getSavingsType() { return savingsType; }
    public void setSavingsType(String savingsType) { this.savingsType = savingsType; }
    public BigDecimal getInvestments() { return investments; }
    public void setInvestments(BigDecimal investments) { this.investments = investments; }
    public JsonNode getInvestmentTypes() { return investmentTypes; }
    public void setInvestmentTypes(JsonNode investmentTypes) { this.investmentTypes = investmentTypes; }
    public BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public String getBudgetStyle() { return budgetStyle; }
    public void setBudgetStyle(String budgetStyle) { this.budgetStyle = budgetStyle; }
    public String getSetupDate() { return setupDate; }
    public void setSetupDate(String setupDate) { this.setupDate = setupDate; }
}
