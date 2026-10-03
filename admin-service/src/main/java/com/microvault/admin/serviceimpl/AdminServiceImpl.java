package com.microvault.admin.serviceimpl;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.client.FinanceDirectory;
import com.microvault.admin.dto.AdminInsightsResponse;
import com.microvault.admin.dto.AdminWorkspaceResponse;
import com.microvault.admin.dto.FeedbackHistoryResponse;
import com.microvault.admin.dto.FeedbackRequest;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.dto.NewsRequest;
import com.microvault.admin.dto.NewsResponse;
import com.microvault.admin.dto.NotificationResponse;
import com.microvault.admin.dto.UserCard;
import com.microvault.admin.dto.UserWriteRequest;
import com.microvault.admin.entity.Feedback;
import com.microvault.admin.entity.FeedbackHistory;
import com.microvault.admin.entity.News;
import com.microvault.admin.entity.Notification;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.repository.FeedbackHistoryRepository;
import com.microvault.admin.repository.FeedbackRepository;
import com.microvault.admin.repository.NewsRepository;
import com.microvault.admin.repository.NotificationRepository;
import com.microvault.admin.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final NotificationRepository notificationRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackHistoryRepository feedbackHistoryRepository;
    private final NewsRepository newsRepository;
    private final AuthDirectory authDirectory;
    private final FinanceDirectory financeDirectory;

    public AdminServiceImpl(NotificationRepository notificationRepository,
                            FeedbackRepository feedbackRepository,
                            FeedbackHistoryRepository feedbackHistoryRepository,
                            NewsRepository newsRepository,
                            AuthDirectory authDirectory,
                            FinanceDirectory financeDirectory) {
        this.notificationRepository = notificationRepository;
        this.feedbackRepository = feedbackRepository;
        this.feedbackHistoryRepository = feedbackHistoryRepository;
        this.newsRepository = newsRepository;
        this.authDirectory = authDirectory;
        this.financeDirectory = financeDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> notifications(UUID userId) {
        List<NotificationResponse> rows = new ArrayList<>();
        for (Notification notification : notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            rows.add(toNotification(notification));
        }
        return rows;
    }

    @Override
    public void markRead(UUID userId, UUID id) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(UUID userId) {
        for (Notification notification : notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            notification.setRead(true);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    public void deleteNotification(UUID userId, UUID id) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        markDeleted(notification);
        notificationRepository.save(notification);
    }

    @Override
    public void deleteAllNotifications(UUID userId) {
        for (Notification notification : notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)) {
            markDeleted(notification);
            notificationRepository.save(notification);
        }
    }

    @Override
    public FeedbackResponse addFeedback(AuthDirectory.SessionUser user, FeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setId(UUID.randomUUID());
        feedback.setUserId(user.getUserId());
        feedback.setSubject(request.getSubject().trim());
        feedback.setCategory(request.getCategory());
        feedback.setMessage(request.getMessage().trim());
        feedback.setStatus(cleanStatus(request.getStatus(), "open"));
        LocalDateTime now = LocalDateTime.now();
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        feedback.setDeleted(false);
        feedbackRepository.save(feedback);
        addHistory(feedback.getId(), user.getUserId(), "created", "Feedback submitted", user.getFullName());
        return toFeedback(feedback, user.getFullName(), user.getEmail());
    }

    @Override
    public FeedbackResponse updateFeedback(AuthDirectory.SessionUser user, UUID id, FeedbackRequest request) {
        Feedback feedback = loadFeedback(id);
        if (!canManage(user, feedback.getUserId())) {
            throw new ForbiddenException("You cannot change this feedback");
        }
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            feedback.setSubject(request.getSubject().trim());
        }
        if (request.getCategory() != null) {
            feedback.setCategory(request.getCategory());
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            feedback.setMessage(request.getMessage().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            feedback.setStatus(cleanStatus(request.getStatus(), feedback.getStatus()));
        }
        feedback.setUpdatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
        addHistory(feedback.getId(), user.getUserId(), "updated", "Feedback updated", user.getFullName());
        return toFeedback(feedback, user.getFullName(), user.getEmail());
    }

    @Override
    public void deleteFeedback(AuthDirectory.SessionUser user, UUID id) {
        Feedback feedback = loadFeedback(id);
        if (!canManage(user, feedback.getUserId())) {
            throw new ForbiddenException("You cannot delete this feedback");
        }
        LocalDateTime now = LocalDateTime.now();
        feedback.setDeleted(true);
        feedback.setDeletedAt(now);
        feedback.setUpdatedAt(now);
        feedbackRepository.save(feedback);
        addHistory(feedback.getId(), user.getUserId(), "deleted", "Feedback removed", user.getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackHistoryResponse> history(UUID feedbackId) {
        loadFeedback(feedbackId);
        Map<UUID, String> names = namesById();
        List<FeedbackHistoryResponse> rows = new ArrayList<>();
        for (FeedbackHistory item : feedbackHistoryRepository.findByFeedbackIdAndDeletedFalseOrderByChangedAtDesc(feedbackId)) {
            FeedbackHistoryResponse response = new FeedbackHistoryResponse();
            response.setAction(item.getAction());
            response.setNote(item.getNote());
            response.setAt(item.getChangedAt() == null ? null : item.getChangedAt().toLocalDate().toString());
            response.setBy(names.getOrDefault(item.getChangedBy(), "—"));
            rows.add(response);
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> allFeedback() {
        return mapFeedback(feedbackRepository.findByDeletedFalseOrderByCreatedAtDesc());
    }

    @Override
    public NewsResponse addNews(AuthDirectory.SessionUser user, NewsRequest request) {
        News news = new News();
        news.setId(UUID.randomUUID());
        news.setAuthorId(user.getUserId());
        applyNews(news, request);
        LocalDateTime now = LocalDateTime.now();
        news.setCreatedAt(now);
        news.setUpdatedAt(now);
        news.setDeleted(false);
        if ("published".equals(news.getStatus())) {
            news.setPublishedAt(now);
        }
        return toNews(newsRepository.save(news), user.getFullName());
    }

    @Override
    public NewsResponse updateNews(UUID id, NewsRequest request) {
        News news = newsRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("News item not found"));
        applyNews(news, request);
        news.setUpdatedAt(LocalDateTime.now());
        if ("published".equals(news.getStatus()) && news.getPublishedAt() == null) {
            news.setPublishedAt(news.getUpdatedAt());
        }
        return toNews(newsRepository.save(news), nameOf(news.getAuthorId()));
    }

    @Override
    public void deleteNews(UUID id) {
        News news = newsRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("News item not found"));
        LocalDateTime now = LocalDateTime.now();
        news.setDeleted(true);
        news.setDeletedAt(now);
        news.setUpdatedAt(now);
        newsRepository.save(news);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsResponse> allNews() {
        return mapNews(newsRepository.findByDeletedFalseOrderByCreatedAtDesc());
    }

    @Override
    public UserCard createAdmin(UserWriteRequest request) {
        request.setRole("admin");
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("active");
        }
        return authDirectory.createUser(request);
    }

    @Override
    public UserCard updateUser(UUID id, UserWriteRequest request) {
        return authDirectory.updateUser(id, request);
    }

    @Override
    public void deleteUser(UUID id) {
        authDirectory.deleteUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCard> members() {
        return withSetup(authDirectory.listUsers("user"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCard> admins() {
        return authDirectory.listUsers("admin");
    }

    @Override
    @Transactional(readOnly = true)
    public AdminWorkspaceResponse workspace(UUID userId, String role) {
        boolean admin = "admin".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
        AdminWorkspaceResponse workspace = new AdminWorkspaceResponse();
        workspace.setNotifications(notifications(userId));

        List<FeedbackResponse> mine = mapFeedback(feedbackRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId));
        List<FeedbackResponse> everyFeedback = admin ? allFeedback() : mine;
        workspace.setFeedback(admin ? everyFeedback : mine);
        workspace.setAllFeedback(everyFeedback);

        List<NewsResponse> published = mapNews(newsRepository.findByStatusIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("published"));
        List<NewsResponse> everyNews = admin ? allNews() : published;
        workspace.setNews(published);
        workspace.setAllNews(everyNews);

        if (admin) {
            List<UserCard> memberRows = members();
            List<UserCard> adminRows = admins();
            workspace.setMembers(memberRows);
            workspace.setAdmins(adminRows);
            workspace.setAdminInsights(insights(memberRows, adminRows, everyFeedback, everyNews));
        }
        return workspace;
    }

    private AdminInsightsResponse insights(List<UserCard> memberRows, List<UserCard> adminRows,
                                           List<FeedbackResponse> feedbackRows, List<NewsResponse> newsRows) {
        int active = 0;
        int setupDone = 0;
        for (UserCard member : memberRows) {
            if ("active".equalsIgnoreCase(member.getStatus())) {
                active = active + 1;
            }
            if (member.isSetupComplete()) {
                setupDone = setupDone + 1;
            }
        }
        int open = 0;
        for (FeedbackResponse feedback : feedbackRows) {
            if ("open".equalsIgnoreCase(feedback.getStatus()) || "in-review".equalsIgnoreCase(feedback.getStatus())) {
                open = open + 1;
            }
        }
        int published = 0;
        for (NewsResponse news : newsRows) {
            if ("published".equalsIgnoreCase(news.getStatus())) {
                published = published + 1;
            }
        }

        AdminInsightsResponse insights = new AdminInsightsResponse();
        insights.setTotalMembers(memberRows.size());
        insights.setActiveMembers(active);
        insights.setInactiveMembers(memberRows.size() - active);
        insights.setAdminCount(adminRows.size());
        insights.setSetupDone(setupDone);
        insights.setSetupPending(memberRows.size() - setupDone);
        insights.setFeedbackTotal(feedbackRows.size());
        insights.setFeedbackOpen(open);
        insights.setNewsTotal(newsRows.size());
        insights.setPublishedNews(published);
        insights.setStatusValues(List.of(active, memberRows.size() - active));
        insights.setSetupValues(List.of(setupDone, memberRows.size() - setupDone));

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        for (int index = 0; index < 6; index++) {
            LocalDate month = cursor.plusMonths(index);
            labels.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            int count = 0;
            for (UserCard member : memberRows) {
                if (member.getCreatedAt() != null && member.getCreatedAt().startsWith(month.toString().substring(0, 7))) {
                    count = count + 1;
                }
            }
            values.add(count);
        }
        insights.setJoinLabels(labels);
        insights.setJoinValues(values);

        List<UserCard> recent = new ArrayList<>();
        int limit = Math.min(8, memberRows.size());
        for (int index = 0; index < limit; index++) {
            recent.add(memberRows.get(index));
        }
        insights.setRecentMembers(recent);
        return insights;
    }

    private List<UserCard> withSetup(List<UserCard> users) {
        Set<UUID> ready = new HashSet<>(financeDirectory.usersWithProfile());
        for (UserCard user : users) {
            user.setSetupComplete(ready.contains(user.getId()));
        }
        return users;
    }

    private void applyNews(News news, NewsRequest request) {
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            news.setTitle(request.getTitle().trim());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            news.setBody(request.getBody().trim());
        }
        String priority = request.getPriority() == null || request.getPriority().isBlank()
                ? (news.getPriority() == null ? "normal" : news.getPriority())
                : request.getPriority().trim().toLowerCase();
        if (!"low".equals(priority) && !"normal".equals(priority) && !"high".equals(priority)) {
            throw new ValidationException("Priority must be low, normal or high");
        }
        String status = request.getStatus() == null || request.getStatus().isBlank()
                ? (news.getStatus() == null ? "draft" : news.getStatus())
                : request.getStatus().trim().toLowerCase();
        if (!"draft".equals(status) && !"published".equals(status) && !"archived".equals(status)) {
            throw new ValidationException("Status must be draft, published or archived");
        }
        news.setPriority(priority);
        news.setStatus(status);
    }

    private String cleanStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String value = status.trim().toLowerCase();
        if (!"open".equals(value) && !"in-review".equals(value) && !"resolved".equals(value) && !"closed".equals(value)) {
            throw new ValidationException("Status must be open, in-review, resolved or closed");
        }
        return value;
    }

    private boolean canManage(AuthDirectory.SessionUser user, UUID ownerId) {
        if (user.getUserId().equals(ownerId)) {
            return true;
        }
        return "admin".equalsIgnoreCase(user.getRole()) || "superadmin".equalsIgnoreCase(user.getRole());
    }

    private Feedback loadFeedback(UUID id) {
        return feedbackRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
    }

    private void addHistory(UUID feedbackId, UUID userId, String action, String note, String ignoredName) {
        FeedbackHistory history = new FeedbackHistory();
        history.setId(UUID.randomUUID());
        history.setFeedbackId(feedbackId);
        history.setAction(action);
        history.setNote(note);
        history.setChangedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        history.setChangedAt(now);
        history.setCreatedAt(now);
        history.setUpdatedAt(now);
        history.setDeleted(false);
        feedbackHistoryRepository.save(history);
    }

    private void markDeleted(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        notification.setDeleted(true);
        notification.setDeletedAt(now);
        notification.setUpdatedAt(now);
    }

    private NotificationResponse toNotification(Notification notification) {
        String day = notification.getCreatedAt() == null ? null : notification.getCreatedAt().toLocalDate().toString();
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setRead(notification.isRead());
        response.setDate(day);
        response.setCreatedAt(day);
        return response;
    }

    private List<FeedbackResponse> mapFeedback(List<Feedback> rows) {
        Map<UUID, UserCard> users = usersById();
        List<FeedbackResponse> responses = new ArrayList<>();
        for (Feedback feedback : rows) {
            UserCard owner = users.get(feedback.getUserId());
            String name = owner == null ? "Member" : owner.getFullName();
            String email = owner == null ? "" : owner.getEmail();
            responses.add(toFeedback(feedback, name, email));
        }
        return responses;
    }

    private FeedbackResponse toFeedback(Feedback feedback, String name, String email) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setUserId(feedback.getUserId());
        response.setUserName(name);
        response.setUserEmail(email);
        response.setSubject(feedback.getSubject());
        response.setCategory(feedback.getCategory());
        response.setMessage(feedback.getMessage());
        response.setStatus(feedback.getStatus());
        if (feedback.getCreatedAt() != null) {
            response.setCreatedAt(feedback.getCreatedAt().toLocalDate().toString());
        }
        return response;
    }

    private List<NewsResponse> mapNews(List<News> rows) {
        Map<UUID, String> names = namesById();
        List<NewsResponse> responses = new ArrayList<>();
        for (News news : rows) {
            responses.add(toNews(news, names.getOrDefault(news.getAuthorId(), "Admin")));
        }
        return responses;
    }

    private NewsResponse toNews(News news, String authorName) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setBody(news.getBody());
        response.setPriority(news.getPriority());
        response.setStatus(news.getStatus());
        response.setAuthorName(authorName);
        if (news.getCreatedAt() != null) {
            response.setCreatedAt(news.getCreatedAt().toLocalDate().toString());
        }
        return response;
    }

    private String nameOf(UUID userId) {
        return namesById().getOrDefault(userId, "Admin");
    }

    private Map<UUID, String> namesById() {
        Map<UUID, String> names = new HashMap<>();
        for (UserCard user : authDirectory.listUsers(null)) {
            names.put(user.getId(), user.getFullName());
        }
        return names;
    }

    private Map<UUID, UserCard> usersById() {
        Map<UUID, UserCard> users = new HashMap<>();
        for (UserCard user : authDirectory.listUsers(null)) {
            users.put(user.getId(), user);
        }
        return users;
    }
}
