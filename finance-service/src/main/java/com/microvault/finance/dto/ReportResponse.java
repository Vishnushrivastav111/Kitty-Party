package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReportResponse {
    private UUID id;
    private String type;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private int txCount;
    private LocalDate createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    public BigDecimal getExpense() { return expense; }
    public void setExpense(BigDecimal expense) { this.expense = expense; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
    public int getTxCount() { return txCount; }
    public void setTxCount(int txCount) { this.txCount = txCount; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}
