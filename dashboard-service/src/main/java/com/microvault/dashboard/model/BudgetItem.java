package com.microvault.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of budgets. limit is the monthly cap. */
@Entity
@Table(name = "budgets")
public class BudgetItem {

    @Id
    @Column(name = "id")
    private UUID id;

    @JsonIgnore
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String category;

    @Column(name = "monthly_limit")
    private BigDecimal limit;

    private String note;

    @JsonIgnore
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
