package com.microvault.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * News data that is safe to hand to another layer or to the UI.
 */
public class NewsDTO {

    private UUID id;
    private UUID authorId;
    private String title;
    private String body;
    private String priority;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public NewsDTO() {
    }

    public NewsDTO(UUID id, UUID authorId, String title, String body, String priority, String status) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.priority = priority;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "NewsDTO{id=" + id
                + ", authorId=" + authorId
                + ", title='" + title + '\''
                + ", priority='" + priority + '\''
                + ", status='" + status + '\''
                + ", publishedAt=" + publishedAt
                + '}';
    }
}
