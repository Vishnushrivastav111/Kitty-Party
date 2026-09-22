package com.microvault.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One audit trail line of a feedback item, ready to be shown on the
 * feedback details screen.
 */
public class FeedbackHistoryDTO {

    private UUID id;
    private UUID feedbackId;
    private String action;
    private String note;
    private UUID changedBy;
    private LocalDateTime changedAt;

    public FeedbackHistoryDTO() {
    }

    public FeedbackHistoryDTO(UUID id, UUID feedbackId, String action, String note, UUID changedBy,
                              LocalDateTime changedAt) {
        this.id = id;
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

    @Override
    public String toString() {
        return "FeedbackHistoryDTO{id=" + id
                + ", feedbackId=" + feedbackId
                + ", action='" + action + '\''
                + ", note='" + note + '\''
                + ", changedBy=" + changedBy
                + ", changedAt=" + changedAt
                + '}';
    }
}
