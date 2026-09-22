package com.microvault.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents one row of the "feedback_history" table, the audit trail of
 * everything that happened to a feedback item.
 */
public class FeedbackHistory {

    private UUID id;
    private UUID feedbackId;
    private String action;
    private String note;
    private UUID changedBy;
    private LocalDateTime changedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    public FeedbackHistory() {
    }

    public FeedbackHistory(UUID feedbackId, String action, String note, UUID changedBy,
                           LocalDateTime changedAt) {
        this.feedbackId = feedbackId;
        this.action = action;
        this.note = note;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(UUID feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "FeedbackHistory{id=" + id
                + ", feedbackId=" + feedbackId
                + ", action='" + action + '\''
                + ", note='" + note + '\''
                + ", changedBy=" + changedBy
                + ", changedAt=" + changedAt
                + ", isDeleted=" + isDeleted
                + '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeedbackHistory)) {
            return false;
        }
        FeedbackHistory feedbackHistory = (FeedbackHistory) other;
        return Objects.equals(id, feedbackHistory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
