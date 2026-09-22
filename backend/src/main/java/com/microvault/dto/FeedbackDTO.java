package com.microvault.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feedback data that is safe to hand to another layer or to the UI.
 * deletedAt is kept here because the admin feedback screen has a "deleted"
 * filter that shows soft-deleted items.
 */
public class FeedbackDTO {

    private UUID id;
    private UUID userId;
    private String subject;
    private String category;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public FeedbackDTO() {
    }

    public FeedbackDTO(UUID id, UUID userId, String subject, String category, String message, String status) {
        this.id = id;
        this.userId = userId;
        this.subject = subject;
        this.category = category;
        this.message = message;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "FeedbackDTO{id=" + id
                + ", userId=" + userId
                + ", subject='" + subject + '\''
                + ", category='" + category + '\''
                + ", status='" + status + '\''
                + '}';
    }
}
