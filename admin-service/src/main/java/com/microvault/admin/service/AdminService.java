package com.microvault.admin.service;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.AdminWorkspaceResponse;
import com.microvault.admin.dto.FeedbackHistoryResponse;
import com.microvault.admin.dto.FeedbackRequest;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.dto.NewsRequest;
import com.microvault.admin.dto.NewsResponse;
import com.microvault.admin.dto.NotificationResponse;
import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;

import java.util.List;
import java.util.UUID;

public interface AdminService {
    List<NotificationResponse> notifications(UUID userId);
    void markRead(UUID userId, UUID id);
    void markAllRead(UUID userId);
    void deleteNotification(UUID userId, UUID id);
    void deleteAllNotifications(UUID userId);

    FeedbackResponse addFeedback(AuthDirectory.SessionUser user, FeedbackRequest request);
    FeedbackResponse updateFeedback(AuthDirectory.SessionUser user, UUID id, FeedbackRequest request);
    void deleteFeedback(AuthDirectory.SessionUser user, UUID id);
    List<FeedbackHistoryResponse> history(UUID feedbackId);
    List<FeedbackResponse> allFeedback();

    NewsResponse addNews(AuthDirectory.SessionUser user, NewsRequest request);
    NewsResponse updateNews(UUID id, NewsRequest request);
    void deleteNews(UUID id);
    List<NewsResponse> allNews();

    UserCard createAdmin(UserWriteRequest request);
    UserCard updateUser(UUID id, UserWriteRequest request);
    void deleteUser(UUID id);
    List<UserCard> members();
    List<UserCard> admins();

    AdminWorkspaceResponse workspace(UUID userId, String role);
}
