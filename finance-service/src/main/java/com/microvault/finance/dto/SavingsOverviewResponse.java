package com.microvault.finance.dto;

import java.math.BigDecimal;

/**
 * The user's savings picture. Allocated, unallocated liquid, and emergency reserve never overlap,
 * so total savings is their sum. Investments are reported separately and are not part of the total.
 */
public class SavingsOverviewResponse {
    private BigDecimal allocatedSavings;
    private BigDecimal unallocatedLiquid;
    private BigDecimal emergencyReserve;
    private BigDecimal totalSavings;
    private BigDecimal investments;
    /** Savings kept back from wants: three months of total monthly income. */
    private BigDecimal protectedForNeeds;
    /** Unallocated liquid savings above the protected amount, usable for a want. */
    private BigDecimal availableForWants;

    public BigDecimal getAllocatedSavings() { return allocatedSavings; }
    public void setAllocatedSavings(BigDecimal allocatedSavings) { this.allocatedSavings = allocatedSavings; }
    public BigDecimal getUnallocatedLiquid() { return unallocatedLiquid; }
    public void setUnallocatedLiquid(BigDecimal unallocatedLiquid) { this.unallocatedLiquid = unallocatedLiquid; }
    public BigDecimal getEmergencyReserve() { return emergencyReserve; }
    public void setEmergencyReserve(BigDecimal emergencyReserve) { this.emergencyReserve = emergencyReserve; }
    public BigDecimal getTotalSavings() { return totalSavings; }
    public void setTotalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; }
    public BigDecimal getInvestments() { return investments; }
    public void setInvestments(BigDecimal investments) { this.investments = investments; }
    public BigDecimal getProtectedForNeeds() { return protectedForNeeds; }
    public void setProtectedForNeeds(BigDecimal protectedForNeeds) { this.protectedForNeeds = protectedForNeeds; }
    public BigDecimal getAvailableForWants() { return availableForWants; }
    public void setAvailableForWants(BigDecimal availableForWants) { this.availableForWants = availableForWants; }
}
