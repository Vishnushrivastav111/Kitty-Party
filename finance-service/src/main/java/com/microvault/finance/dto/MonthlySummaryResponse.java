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
}
