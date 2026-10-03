package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetResponse {
    private UUID id;
    private String category;
    private BigDecimal limit;
    private String note;
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal usedPercent;
    private String usage;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }
    public BigDecimal getUsedPercent() { return usedPercent; }
    public void setUsedPercent(BigDecimal usedPercent) { this.usedPercent = usedPercent; }
    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
}
