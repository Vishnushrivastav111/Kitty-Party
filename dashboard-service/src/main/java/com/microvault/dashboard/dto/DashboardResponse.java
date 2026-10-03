package com.microvault.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class DashboardResponse {
    private JsonNode user;
    private JsonNode finance;
    private DashboardStats stats;
    private JsonNode transactions;
    private JsonNode goals;
    private JsonNode savings;
    private JsonNode budgets;
    private JsonNode reports;
    private JsonNode affordChecks;
    private JsonNode notifications;
    private JsonNode feedback;
    private JsonNode allFeedback;
    private JsonNode news;
    private JsonNode allNews;
    private JsonNode members;
    private JsonNode admins;
    private JsonNode adminInsights;
    private boolean setupSkipped;

    public JsonNode getUser() { return user; }
    public void setUser(JsonNode user) { this.user = user; }
    public JsonNode getFinance() { return finance; }
    public void setFinance(JsonNode finance) { this.finance = finance; }
    public DashboardStats getStats() { return stats; }
    public void setStats(DashboardStats stats) { this.stats = stats; }
    public JsonNode getTransactions() { return transactions; }
    public void setTransactions(JsonNode transactions) { this.transactions = transactions; }
    public JsonNode getGoals() { return goals; }
    public void setGoals(JsonNode goals) { this.goals = goals; }
    public JsonNode getSavings() { return savings; }
    public void setSavings(JsonNode savings) { this.savings = savings; }
    public JsonNode getBudgets() { return budgets; }
    public void setBudgets(JsonNode budgets) { this.budgets = budgets; }
    public JsonNode getReports() { return reports; }
    public void setReports(JsonNode reports) { this.reports = reports; }
    public JsonNode getAffordChecks() { return affordChecks; }
    public void setAffordChecks(JsonNode affordChecks) { this.affordChecks = affordChecks; }
    public JsonNode getNotifications() { return notifications; }
    public void setNotifications(JsonNode notifications) { this.notifications = notifications; }
    public JsonNode getFeedback() { return feedback; }
    public void setFeedback(JsonNode feedback) { this.feedback = feedback; }
    public JsonNode getAllFeedback() { return allFeedback; }
    public void setAllFeedback(JsonNode allFeedback) { this.allFeedback = allFeedback; }
    public JsonNode getNews() { return news; }
    public void setNews(JsonNode news) { this.news = news; }
    public JsonNode getAllNews() { return allNews; }
    public void setAllNews(JsonNode allNews) { this.allNews = allNews; }
    public JsonNode getMembers() { return members; }
    public void setMembers(JsonNode members) { this.members = members; }
    public JsonNode getAdmins() { return admins; }
    public void setAdmins(JsonNode admins) { this.admins = admins; }
    public JsonNode getAdminInsights() { return adminInsights; }
    public void setAdminInsights(JsonNode adminInsights) { this.adminInsights = adminInsights; }
    public boolean isSetupSkipped() { return setupSkipped; }
    public void setSetupSkipped(boolean setupSkipped) { this.setupSkipped = setupSkipped; }

    public static List<String> emptyNote() {
        return new ArrayList<>();
    }
}
