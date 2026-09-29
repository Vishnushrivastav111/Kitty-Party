package com.microvault.financeprofile.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Income minus monthly expenses. Never negative: a shortfall is returned as zero.
 */
public class SurplusResponse {

    private UUID userId;
    private BigDecimal monthlySurplus;

    public SurplusResponse() {
    }

    public SurplusResponse(UUID userId, BigDecimal monthlySurplus) {
        this.userId = userId;
        this.monthlySurplus = monthlySurplus;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getMonthlySurplus() {
        return monthlySurplus;
    }

    public void setMonthlySurplus(BigDecimal monthlySurplus) {
        this.monthlySurplus = monthlySurplus;
    }
}
