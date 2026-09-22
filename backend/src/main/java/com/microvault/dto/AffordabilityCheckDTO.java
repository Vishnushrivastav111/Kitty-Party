package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Affordability check data that is safe to hand to another layer or to the UI.
 * The soft delete flags are never part of this class.
 */
public class AffordabilityCheckDTO {

    private UUID id;
    private UUID userId;
    private String itemName;
    private BigDecimal amount;
    private BigDecimal availableAmount;
    private String verdict;
    private String level;
    private String priority;
    private LocalDate checkDate;
    private LocalDateTime createdAt;

    public AffordabilityCheckDTO() {
    }

    public AffordabilityCheckDTO(UUID id, UUID userId, String itemName, BigDecimal amount,
                                 BigDecimal availableAmount, String verdict, String level,
                                 String priority, LocalDate checkDate) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.amount = amount;
        this.availableAmount = availableAmount;
        this.verdict = verdict;
        this.level = level;
        this.priority = priority;
        this.checkDate = checkDate;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(BigDecimal availableAmount) {
        this.availableAmount = availableAmount;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AffordabilityCheckDTO{id=" + id
                + ", userId=" + userId
                + ", itemName='" + itemName + '\''
                + ", amount=" + amount
                + ", availableAmount=" + availableAmount
                + ", verdict='" + verdict + '\''
                + ", level='" + level + '\''
                + ", priority='" + priority + '\''
                + ", checkDate=" + checkDate
                + '}';
    }
}
