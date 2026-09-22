package com.microvault.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Goal data that is safe to hand to another layer or to the UI. The progress
 * percentage and the remaining amount are filled in by the service layer.
 */
public class GoalDTO {

    private UUID id;
    private UUID userId;
    private String title;
    private String category;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private LocalDate deadline;
    private String status;
    private BigDecimal progressPercentage;
    private BigDecimal remainingAmount;
    private LocalDateTime createdAt;

    public GoalDTO() {
    }

    public GoalDTO(UUID id, UUID userId, String title, String category, BigDecimal targetAmount,
                   BigDecimal savedAmount, LocalDate deadline, String status) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.deadline = deadline;
        this.status = status;
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

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.savedAmount = savedAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GoalDTO{id=" + id
                + ", userId=" + userId
                + ", title='" + title + '\''
                + ", category='" + category + '\''
                + ", targetAmount=" + targetAmount
                + ", savedAmount=" + savedAmount
                + ", deadline=" + deadline
                + ", status='" + status + '\''
                + ", progressPercentage=" + progressPercentage
                + ", remainingAmount=" + remainingAmount
                + '}';
    }
}
