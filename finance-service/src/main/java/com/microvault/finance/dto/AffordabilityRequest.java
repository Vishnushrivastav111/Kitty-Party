package com.microvault.finance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AffordabilityRequest {

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Check date is required")
    @JsonAlias("date")
    private LocalDate checkDate;

    private String priority;
    private String paymentMethod;
    private Integer emiMonths;
    private BigDecimal annualInterestPercent;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Integer getEmiMonths() { return emiMonths; }
    public void setEmiMonths(Integer emiMonths) { this.emiMonths = emiMonths; }
    public BigDecimal getAnnualInterestPercent() { return annualInterestPercent; }
    public void setAnnualInterestPercent(BigDecimal annualInterestPercent) { this.annualInterestPercent = annualInterestPercent; }
}
