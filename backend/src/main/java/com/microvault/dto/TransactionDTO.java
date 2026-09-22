package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction data that is safe to hand to another layer or to the UI.
 * The audit flags of the table are not part of this class.
 */
public class TransactionDTO {

    private UUID id;
    private UUID userId;
    private String name;
    private String category;
    private String type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String note;
    private LocalDateTime createdAt;

    public TransactionDTO() {
    }

    public TransactionDTO(UUID id, UUID userId, String name, String category, String type,
                          BigDecimal amount, LocalDate transactionDate, String note) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
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
        return "TransactionDTO{id=" + id
                + ", userId=" + userId
                + ", name='" + name + '\''
                + ", category='" + category + '\''
                + ", type='" + type + '\''
                + ", amount=" + amount
                + ", transactionDate=" + transactionDate
                + '}';
    }
}
