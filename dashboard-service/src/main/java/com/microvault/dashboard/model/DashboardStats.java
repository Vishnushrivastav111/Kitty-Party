package com.microvault.dashboard.model;

import java.math.BigDecimal;

/** The four summary cards: health, savings, budget and active goals. */
public class DashboardStats {

    private String fullName;
    private int health;
    private BigDecimal totalSavings;
    private BigDecimal monthBudget;
    private BigDecimal spent;
    private int activeGoals;
    private BigDecimal income;
    private BigDecimal expenses;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(BigDecimal totalSavings) {
        this.totalSavings = totalSavings;
    }

    public BigDecimal getMonthBudget() {
        return monthBudget;
    }

    public void setMonthBudget(BigDecimal monthBudget) {
        this.monthBudget = monthBudget;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public void setSpent(BigDecimal spent) {
        this.spent = spent;
    }

    public int getActiveGoals() {
        return activeGoals;
    }

    public void setActiveGoals(int activeGoals) {
        this.activeGoals = activeGoals;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public void setExpenses(BigDecimal expenses) {
        this.expenses = expenses;
    }
}
