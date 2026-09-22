package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Savings entry data that is safe to hand to another layer or to the UI.
 * The audit flags of the table are not part of this class.
 */
public class SavingsEntryDTO {

    private UUID id;
    private UUID userId;
    private String title;
    private String category;
    private BigDecimal amount;
    private LocalDate entryDate;
    private String note;
    private LocalDateTime createdAt;

    public SavingsEntryDTO() {
    }

    public SavingsEntryDTO(UUID id, UUID userId, String title, String category,
                           BigDecimal amount, LocalDate entryDate, String note) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.entryDate = entryDate;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
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
        return "SavingsEntryDTO{id=" + id
                + ", userId=" + userId
                + ", title='" + title + '\''
                + ", category='" + category + '\''
                + ", amount=" + amount
                + ", entryDate=" + entryDate
                + '}';
    }
}
