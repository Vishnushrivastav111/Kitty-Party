package com.microvault.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminInsightsResponse {
    private int totalMembers;
    private int activeMembers;
    private int inactiveMembers;
    private int adminCount;
    private int setupDone;
    private int setupPending;
    private int feedbackTotal;
    private int feedbackOpen;
    private int newsTotal;
    private int publishedNews;
    private List<String> joinLabels = new ArrayList<>();
    private List<Integer> joinValues = new ArrayList<>();
    private List<String> statusLabels = List.of("Active", "Inactive");
    private List<Integer> statusValues = new ArrayList<>();
    private List<String> setupLabels = List.of("Setup done", "Pending");
    private List<Integer> setupValues = new ArrayList<>();
    private List<UserCard> recentMembers = new ArrayList<>();

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
    public int getActiveMembers() { return activeMembers; }
    public void setActiveMembers(int activeMembers) { this.activeMembers = activeMembers; }
    public int getInactiveMembers() { return inactiveMembers; }
    public void setInactiveMembers(int inactiveMembers) { this.inactiveMembers = inactiveMembers; }
    public int getAdminCount() { return adminCount; }
    public void setAdminCount(int adminCount) { this.adminCount = adminCount; }
    public int getSetupDone() { return setupDone; }
    public void setSetupDone(int setupDone) { this.setupDone = setupDone; }
    public int getSetupPending() { return setupPending; }
    public void setSetupPending(int setupPending) { this.setupPending = setupPending; }
    public int getFeedbackTotal() { return feedbackTotal; }
    public void setFeedbackTotal(int feedbackTotal) { this.feedbackTotal = feedbackTotal; }
    public int getFeedbackOpen() { return feedbackOpen; }
    public void setFeedbackOpen(int feedbackOpen) { this.feedbackOpen = feedbackOpen; }
    public int getNewsTotal() { return newsTotal; }
    public void setNewsTotal(int newsTotal) { this.newsTotal = newsTotal; }
    public int getPublishedNews() { return publishedNews; }
    public void setPublishedNews(int publishedNews) { this.publishedNews = publishedNews; }
    public List<String> getJoinLabels() { return joinLabels; }
    public void setJoinLabels(List<String> joinLabels) { this.joinLabels = joinLabels; }
    public List<Integer> getJoinValues() { return joinValues; }
    public void setJoinValues(List<Integer> joinValues) { this.joinValues = joinValues; }
    public List<String> getStatusLabels() { return statusLabels; }
    public void setStatusLabels(List<String> statusLabels) { this.statusLabels = statusLabels; }
    public List<Integer> getStatusValues() { return statusValues; }
    public void setStatusValues(List<Integer> statusValues) { this.statusValues = statusValues; }
    public List<String> getSetupLabels() { return setupLabels; }
    public void setSetupLabels(List<String> setupLabels) { this.setupLabels = setupLabels; }
    public List<Integer> getSetupValues() { return setupValues; }
    public void setSetupValues(List<Integer> setupValues) { this.setupValues = setupValues; }
    public List<UserCard> getRecentMembers() { return recentMembers; }
    public void setRecentMembers(List<UserCard> recentMembers) { this.recentMembers = recentMembers; }
}
