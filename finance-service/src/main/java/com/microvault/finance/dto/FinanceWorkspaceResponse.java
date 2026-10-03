package com.microvault.finance.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceWorkspaceResponse {
    private FinanceProfileResponse finance;
    private List<TransactionResponse> transactions = new ArrayList<>();
    private List<GoalResponse> goals = new ArrayList<>();
    private List<SavingsResponse> savings = new ArrayList<>();
    private List<BudgetResponse> budgets = new ArrayList<>();
    private List<ReportResponse> reports = new ArrayList<>();
    private List<AffordabilityResponse> affordChecks = new ArrayList<>();
    private boolean setupSkipped;

    public FinanceProfileResponse getFinance() { return finance; }
    public void setFinance(FinanceProfileResponse finance) { this.finance = finance; }
    public List<TransactionResponse> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionResponse> transactions) { this.transactions = transactions; }
    public List<GoalResponse> getGoals() { return goals; }
    public void setGoals(List<GoalResponse> goals) { this.goals = goals; }
    public List<SavingsResponse> getSavings() { return savings; }
    public void setSavings(List<SavingsResponse> savings) { this.savings = savings; }
    public List<BudgetResponse> getBudgets() { return budgets; }
    public void setBudgets(List<BudgetResponse> budgets) { this.budgets = budgets; }
    public List<ReportResponse> getReports() { return reports; }
    public void setReports(List<ReportResponse> reports) { this.reports = reports; }
    public List<AffordabilityResponse> getAffordChecks() { return affordChecks; }
    public void setAffordChecks(List<AffordabilityResponse> affordChecks) { this.affordChecks = affordChecks; }
    public boolean isSetupSkipped() { return setupSkipped; }
    public void setSetupSkipped(boolean setupSkipped) { this.setupSkipped = setupSkipped; }
}
