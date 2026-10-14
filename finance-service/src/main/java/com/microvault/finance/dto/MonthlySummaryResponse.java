package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MonthlySummaryResponse {
    private UUID userId;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlyEmi;
    private BigDecimal monthlySurplus;
    private BigDecimal savingsRatePercent;
    private BigDecimal planningCapacity;
    private BigDecimal totalMonthlyIncome;
    private BigDecimal otherCommitments;
    private BigDecimal budgetCapacity;
    private BigDecimal remainingBudgetCapacity;
    private BigDecimal goalContributionCapacity;
    private BigDecimal plannedGoalContributions;
    private BigDecimal emergencyReserve;
    private BigDecimal unallocatedLiquid;
    private BigDecimal monthlyConsumption;
    private java.util.List<String> insights = new java.util.ArrayList<>();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }
    public BigDecimal getMonthlyEmi() { return monthlyEmi; }
    public void setMonthlyEmi(BigDecimal monthlyEmi) { this.monthlyEmi = monthlyEmi; }
    public BigDecimal getMonthlySurplus() { return monthlySurplus; }
    public void setMonthlySurplus(BigDecimal monthlySurplus) { this.monthlySurplus = monthlySurplus; }
    public BigDecimal getSavingsRatePercent() { return savingsRatePercent; }
    public void setSavingsRatePercent(BigDecimal savingsRatePercent) { this.savingsRatePercent = savingsRatePercent; }
    public BigDecimal getPlanningCapacity() { return planningCapacity; }
    public void setPlanningCapacity(BigDecimal planningCapacity) { this.planningCapacity = planningCapacity; }
    public BigDecimal getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public void setTotalMonthlyIncome(BigDecimal totalMonthlyIncome) { this.totalMonthlyIncome = totalMonthlyIncome; }
    public BigDecimal getOtherCommitments() { return otherCommitments; }
    public void setOtherCommitments(BigDecimal otherCommitments) { this.otherCommitments = otherCommitments; }
    public BigDecimal getBudgetCapacity() { return budgetCapacity; }
    public void setBudgetCapacity(BigDecimal budgetCapacity) { this.budgetCapacity = budgetCapacity; }
    public BigDecimal getRemainingBudgetCapacity() { return remainingBudgetCapacity; }
    public void setRemainingBudgetCapacity(BigDecimal remainingBudgetCapacity) { this.remainingBudgetCapacity = remainingBudgetCapacity; }
    public BigDecimal getGoalContributionCapacity() { return goalContributionCapacity; }
    public void setGoalContributionCapacity(BigDecimal goalContributionCapacity) { this.goalContributionCapacity = goalContributionCapacity; }
    public BigDecimal getPlannedGoalContributions() { return plannedGoalContributions; }
    public void setPlannedGoalContributions(BigDecimal plannedGoalContributions) { this.plannedGoalContributions = plannedGoalContributions; }
    public BigDecimal getEmergencyReserve() { return emergencyReserve; }
    public void setEmergencyReserve(BigDecimal emergencyReserve) { this.emergencyReserve = emergencyReserve; }
    public BigDecimal getUnallocatedLiquid() { return unallocatedLiquid; }
    public void setUnallocatedLiquid(BigDecimal unallocatedLiquid) { this.unallocatedLiquid = unallocatedLiquid; }
    public BigDecimal getMonthlyConsumption() { return monthlyConsumption; }
    public void setMonthlyConsumption(BigDecimal monthlyConsumption) { this.monthlyConsumption = monthlyConsumption; }
    public java.util.List<String> getInsights() { return insights; }
    public void setInsights(java.util.List<String> insights) { this.insights = insights; }
}
