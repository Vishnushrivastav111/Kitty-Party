package com.microvault.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkspaceResponse {
    private List<NotificationResponse> notifications = new ArrayList<>();
    private List<FeedbackResponse> feedback = new ArrayList<>();
    private List<FeedbackResponse> allFeedback = new ArrayList<>();
    private List<NewsResponse> news = new ArrayList<>();
    private List<NewsResponse> allNews = new ArrayList<>();
    private List<UserCard> members = new ArrayList<>();
    private List<UserCard> admins = new ArrayList<>();
    private AdminInsightsResponse adminInsights;

    public List<NotificationResponse> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationResponse> notifications) { this.notifications = notifications; }
    public List<FeedbackResponse> getFeedback() { return feedback; }
    public void setFeedback(List<FeedbackResponse> feedback) { this.feedback = feedback; }
    public List<FeedbackResponse> getAllFeedback() { return allFeedback; }
    public void setAllFeedback(List<FeedbackResponse> allFeedback) { this.allFeedback = allFeedback; }
    public List<NewsResponse> getNews() { return news; }
    public void setNews(List<NewsResponse> news) { this.news = news; }
    public List<NewsResponse> getAllNews() { return allNews; }
    public void setAllNews(List<NewsResponse> allNews) { this.allNews = allNews; }
    public List<UserCard> getMembers() { return members; }
    public void setMembers(List<UserCard> members) { this.members = members; }
    public List<UserCard> getAdmins() { return admins; }
    public void setAdmins(List<UserCard> admins) { this.admins = admins; }
    public AdminInsightsResponse getAdminInsights() { return adminInsights; }
    public void setAdminInsights(AdminInsightsResponse adminInsights) { this.adminInsights = adminInsights; }
}
