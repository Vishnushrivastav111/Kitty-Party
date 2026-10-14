package com.microvault.finance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String category;

    @NotNull(message = "Target is required")
    @Positive(message = "Target must be greater than zero")
    private BigDecimal target;

    @NotNull(message = "Saved amount is required")
    private BigDecimal saved;

    private LocalDate deadline;
    private String status;
    private String description;
    private String priority;
    private BigDecimal plannedContribution;
    private Boolean allocateExisting;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getTarget() { return target; }
    public void setTarget(BigDecimal target) { this.target = target; }
    public BigDecimal getSaved() { return saved; }
    public void setSaved(BigDecimal saved) { this.saved = saved; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public BigDecimal getPlannedContribution() { return plannedContribution; }
    public void setPlannedContribution(BigDecimal plannedContribution) { this.plannedContribution = plannedContribution; }
    public Boolean getAllocateExisting() { return allocateExisting; }
    public void setAllocateExisting(Boolean allocateExisting) { this.allocateExisting = allocateExisting; }
}
