package com.microvault.financeprofile.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Numbers the dashboard needs from a profile: income, spend, EMI and what is left.
 */
public class MonthlySummaryResponse {

    private UUID profileId;
    private UUID userId;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlyEmi;
    private BigDecimal monthlyBudget;
    private BigDecimal currentSavings;
    private BigDecimal investments;
    private BigDecimal monthlySurplus;
    private BigDecimal leftAfterEmi;
    private BigDecimal savingsRatePercent;

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyEmi;
    }

    public void setMonthlyEmi(BigDecimal monthlyEmi) {
        this.monthlyEmi = monthlyEmi;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getCurrentSavings() {
        return currentSavings;
    }

    public void setCurrentSavings(BigDecimal currentSavings) {
        this.currentSavings = currentSavings;
    }

    public BigDecimal getInvestments() {
        return investments;
    }

    public void setInvestments(BigDecimal investments) {
        this.investments = investments;
    }

    public BigDecimal getMonthlySurplus() {
        return monthlySurplus;
    }

    public void setMonthlySurplus(BigDecimal monthlySurplus) {
        this.monthlySurplus = monthlySurplus;
    }

    public BigDecimal getLeftAfterEmi() {
        return leftAfterEmi;
    }

    public void setLeftAfterEmi(BigDecimal leftAfterEmi) {
        this.leftAfterEmi = leftAfterEmi;
    }

    public BigDecimal getSavingsRatePercent() {
        return savingsRatePercent;
    }

    public void setSavingsRatePercent(BigDecimal savingsRatePercent) {
        this.savingsRatePercent = savingsRatePercent;
    }
}
