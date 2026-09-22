package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget data that is safe to hand to another layer or to the UI.
 * The audit flags of the table are not part of this class.
 */
public class BudgetDTO {

    private UUID id;
    private UUID userId;
    private String category;
    private BigDecimal monthlyLimit;
    private String note;
    private LocalDateTime createdAt;

    public BudgetDTO() {
    }

    public BudgetDTO(UUID id, UUID userId, String category, BigDecimal monthlyLimit, String note) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.note = note;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BudgetDTO{id=" + id
                + ", userId=" + userId
                + ", category='" + category + '\''
                + ", monthlyLimit=" + monthlyLimit
                + '}';
    }
}
