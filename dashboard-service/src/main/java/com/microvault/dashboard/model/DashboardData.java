package com.microvault.dashboard.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole dashboard in one object.
 * JSON names stay user, finance, transactions, goals, savings and budgets
 * so the page can drop the response straight into its cache.
 */
public class DashboardData {

    private Member user;
    private FinanceSnapshot finance;
    private DashboardStats stats;
    private List<TransactionItem> transactions = new ArrayList<>();
    private List<GoalItem> goals = new ArrayList<>();
    private List<SavingItem> savings = new ArrayList<>();
    private List<BudgetItem> budgets = new ArrayList<>();

    public Member getUser() {
        return user;
    }

    public void setUser(Member user) {
        this.user = user;
    }

    public FinanceSnapshot getFinance() {
        return finance;
    }

    public void setFinance(FinanceSnapshot finance) {
        this.finance = finance;
    }

    public DashboardStats getStats() {
        return stats;
    }

    public void setStats(DashboardStats stats) {
        this.stats = stats;
    }

    public List<TransactionItem> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionItem> transactions) {
        this.transactions = transactions;
    }

    public List<GoalItem> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalItem> goals) {
        this.goals = goals;
    }

    public List<SavingItem> getSavings() {
        return savings;
    }

    public void setSavings(List<SavingItem> savings) {
        this.savings = savings;
    }

    public List<BudgetItem> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BudgetItem> budgets) {
        this.budgets = budgets;
    }
}
