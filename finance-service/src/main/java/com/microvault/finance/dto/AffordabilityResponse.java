package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AffordabilityResponse {

    private UUID id;
    private String itemName;
    private BigDecimal amount;
    private BigDecimal available;
    private BigDecimal availableAmount;
    private String verdict;
    private String level;
    private String priority;
    private LocalDate date;
    private String suggestion;
    private List<String> plan = new ArrayList<>();
    private BigDecimal shortfall;
    private Integer monthsToSave;
    private BigDecimal monthlyInstallment;
    private BigDecimal totalRepayment;
    private String paymentMethod;
    private boolean canCreateGoal;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAvailable() { return available; }
    public void setAvailable(BigDecimal available) { this.available = available; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) { this.availableAmount = availableAmount; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public List<String> getPlan() { return plan; }
    public void setPlan(List<String> plan) { this.plan = plan; }
    public BigDecimal getShortfall() { return shortfall; }
    public void setShortfall(BigDecimal shortfall) { this.shortfall = shortfall; }
    public Integer getMonthsToSave() { return monthsToSave; }
    public void setMonthsToSave(Integer monthsToSave) { this.monthsToSave = monthsToSave; }
    public BigDecimal getMonthlyInstallment() { return monthlyInstallment; }
    public void setMonthlyInstallment(BigDecimal monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; }
    public BigDecimal getTotalRepayment() { return totalRepayment; }
    public void setTotalRepayment(BigDecimal totalRepayment) { this.totalRepayment = totalRepayment; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isCanCreateGoal() { return canCreateGoal; }
    public void setCanCreateGoal(boolean canCreateGoal) { this.canCreateGoal = canCreateGoal; }
}
