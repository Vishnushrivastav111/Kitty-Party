package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Report data that is safe to hand to another layer or to the UI.
 * The soft delete flags are never part of this class.
 */
public class ReportDTO {

    private UUID id;
    private UUID userId;
    private String reportType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netAmount;
    private Integer transactionCount;
    private LocalDateTime createdAt;

    public ReportDTO() {
    }

    public ReportDTO(UUID id, UUID userId, String reportType, LocalDate fromDate, LocalDate toDate,
                     BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netAmount,
                     Integer transactionCount) {
        this.id = id;
        this.userId = userId;
        this.reportType = reportType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netAmount = netAmount;
        this.transactionCount = transactionCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ReportDTO{id=" + id
                + ", userId=" + userId
                + ", reportType='" + reportType + '\''
                + ", fromDate=" + fromDate
                + ", toDate=" + toDate
                + ", totalIncome=" + totalIncome
                + ", totalExpense=" + totalExpense
                + ", netAmount=" + netAmount
                + ", transactionCount=" + transactionCount
                + '}';
    }
}
