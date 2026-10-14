package com.microvault.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class GoalResponse {

    private UUID id;
    private String title;
    private String category;
    private BigDecimal target;
    private BigDecimal saved;
    private LocalDate deadline;
    private String status;
    private BigDecimal progressPercent;
    private BigDecimal remaining;
    private String description;
    private String priority;
    private BigDecimal plannedContribution;
    private BigDecimal requiredMonthly;
    private Integer contributionPeriods;
    private boolean feasible;
    private String feasibilityMessage;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public BigDecimal getProgressPercent() { return progressPercent; }
    public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }
    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public BigDecimal getPlannedContribution() { return plannedContribution; }
    public void setPlannedContribution(BigDecimal plannedContribution) { this.plannedContribution = plannedContribution; }
    public BigDecimal getRequiredMonthly() { return requiredMonthly; }
    public void setRequiredMonthly(BigDecimal requiredMonthly) { this.requiredMonthly = requiredMonthly; }
    public Integer getContributionPeriods() { return contributionPeriods; }
    public void setContributionPeriods(Integer contributionPeriods) { this.contributionPeriods = contributionPeriods; }
    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }
    public String getFeasibilityMessage() { return feasibilityMessage; }
    public void setFeasibilityMessage(String feasibilityMessage) { this.feasibilityMessage = feasibilityMessage; }
}
