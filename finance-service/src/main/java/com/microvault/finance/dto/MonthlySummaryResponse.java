package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class MonthlySummaryResponse {
    private UUID userId;
    private BigDecimal monthlyIncome;
    private BigDecimal totalMonthlyIncome;
    private BigDecimal allocatedBudgets;
    private BigDecimal budgetCapacity;
    private BigDecimal remainingBudgetCapacity;
    private BigDecimal goalContributionCapacity;
    private BigDecimal plannedGoalContributions;
    private BigDecimal emergencyReserve;
    private BigDecimal unallocatedLiquid;
    private java.util.List<String> insights = new java.util.ArrayList<>();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public BigDecimal getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public void setTotalMonthlyIncome(BigDecimal totalMonthlyIncome) { this.totalMonthlyIncome = totalMonthlyIncome; }
    public BigDecimal getAllocatedBudgets() { return allocatedBudgets; }
    public void setAllocatedBudgets(BigDecimal allocatedBudgets) { this.allocatedBudgets = allocatedBudgets; }
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
    public java.util.List<String> getInsights() { return insights; }
    public void setInsights(java.util.List<String> insights) { this.insights = insights; }
}
