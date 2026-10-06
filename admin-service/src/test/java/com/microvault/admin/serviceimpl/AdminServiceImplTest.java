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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ITEM = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackHistoryRepository feedbackHistoryRepository;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private AuthDirectory authDirectory;
    @Mock
    private FinanceDirectory financeDirectory;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminServiceImpl(notificationRepository, feedbackRepository, feedbackHistoryRepository,
                newsRepository, authDirectory, financeDirectory);
    }

    // ------------------------------------------------------------------ helpers

    private AuthDirectory.SessionUser session(UUID id, String role) {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(id);
        user.setFullName("Asha Rao");
        user.setEmail("asha@example.com");
        user.setRole(role);
        return user;
    }

    private UserCard card(UUID id, String name, String status, String createdAt) {
        UserCard card = new UserCard();
        card.setId(id);
        card.setFullName(name);
        card.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        card.setStatus(status);
        card.setCreatedAt(createdAt);
        return card;
    }

    private Notification notification(UUID id, UUID userId, LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setType("info");
        notification.setRead(false);
        notification.setCreatedAt(createdAt);
        return notification;
    }

    private Feedback feedback(UUID id, UUID userId, String status, LocalDateTime createdAt) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setUserId(userId);
        feedback.setSubject("Old subject");
        feedback.setCategory("Bug");
        feedback.setMessage("Old message");
        feedback.setStatus(status);
        feedback.setCreatedAt(createdAt);
        return feedback;
    }

    private News news(UUID id, String priority, String status, LocalDateTime publishedAt) {
        News news = new News();
        news.setId(id);
        news.setAuthorId(OWNER);
        news.setTitle("Old title");
        news.setBody("Old body");
        news.setPriority(priority);
        news.setStatus(status);
        news.setPublishedAt(publishedAt);
        news.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
        return news;
    }

    private FeedbackRequest feedbackRequest(String subject, String category, String message, String status) {
        FeedbackRequest request = new FeedbackRequest();
        request.setSubject(subject);
        request.setCategory(category);
        request.setMessage(message);
        request.setStatus(status);
        return request;
    }

    private NewsRequest newsRequest(String title, String body, String priority, String status) {
        NewsRequest request = new NewsRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setPriority(priority);
        request.setStatus(status);
        return request;
    }

    private void echoNews() {
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------ notifications

    @Test
    void notificationsAreMappedWithTheirDate() {
        Notification dated = notification(ITEM, OWNER, LocalDateTime.of(2026, 3, 4, 10, 0));
        Notification undated = notification(OTHER, OWNER, null);
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(dated, undated));

        List<NotificationResponse> rows = service.notifications(OWNER);

        assertEquals(2, rows.size());
        assertEquals(ITEM, rows.get(0).getId());
        assertEquals("Title", rows.get(0).getTitle());
        assertEquals("Message", rows.get(0).getMessage());
        assertEquals("info", rows.get(0).getType());
        assertFalse(rows.get(0).isRead());
        assertEquals("2026-03-04", rows.get(0).getDate());
        assertEquals("2026-03-04", rows.get(0).getCreatedAt());
        assertNull(rows.get(1).getDate());
        assertNull(rows.get(1).getCreatedAt());
    }

    @Test
    void noNotificationsGivesAnEmptyList() {
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER)).thenReturn(List.of());

        assertTrue(service.notifications(OWNER).isEmpty());
    }

    @Test
    void markReadFlagsTheNotificationAndSavesIt() {
        Notification notification = notification(ITEM, OWNER, null);
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(ITEM, OWNER)).thenReturn(Optional.of(notification));

        service.markRead(OWNER, ITEM);

        assertTrue(notification.isRead());
        assertNotNull(notification.getUpdatedAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markReadOfAnUnknownNotificationIsNotFound() {
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(ITEM, OWNER)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> service.markRead(OWNER, ITEM));

        assertEquals("Notification not found", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllReadFlagsEveryNotification() {
        Notification first = notification(ITEM, OWNER, null);
        Notification second = notification(OTHER, OWNER, null);
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(first, second));

        service.markAllRead(OWNER);

        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(notificationRepository).save(first);
        verify(notificationRepository).save(second);
    }

    @Test
    void markAllReadWithoutNotificationsSavesNothing() {
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER)).thenReturn(List.of());

        service.markAllRead(OWNER);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteNotificationIsASoftDelete() {
        Notification notification = notification(ITEM, OWNER, null);
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(ITEM, OWNER)).thenReturn(Optional.of(notification));

        service.deleteNotification(OWNER, ITEM);

        assertTrue(notification.isDeleted());
        assertNotNull(notification.getDeletedAt());
        assertNotNull(notification.getUpdatedAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void deleteUnknownNotificationIsNotFound() {
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(ITEM, OWNER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteNotification(OWNER, ITEM));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteAllNotificationsSoftDeletesEachOne() {
        Notification first = notification(ITEM, OWNER, null);
        Notification second = notification(OTHER, OWNER, null);
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(first, second));

        service.deleteAllNotifications(OWNER);

        assertTrue(first.isDeleted());
        assertTrue(second.isDeleted());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void deleteAllNotificationsWithNoneSavesNothing() {
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER)).thenReturn(List.of());

        service.deleteAllNotifications(OWNER);

        verify(notificationRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ feedback

    @Test
    void addFeedbackTrimsTextDefaultsTheStatusAndWritesHistory() {
        FeedbackResponse response = service.addFeedback(session(OWNER, "user"),
                feedbackRequest("  App idea ", "Idea", " Please add export  ", null));

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        Feedback feedback = saved.getValue();
        assertNotNull(feedback.getId());
        assertEquals(OWNER, feedback.getUserId());
        assertEquals("App idea", feedback.getSubject());
        assertEquals("Please add export", feedback.getMessage());
        assertEquals("Idea", feedback.getCategory());
        assertEquals("open", feedback.getStatus());
        assertFalse(feedback.isDeleted());
        assertNotNull(feedback.getCreatedAt());

        ArgumentCaptor<FeedbackHistory> history = ArgumentCaptor.forClass(FeedbackHistory.class);
        verify(feedbackHistoryRepository).save(history.capture());
        assertEquals(feedback.getId(), history.getValue().getFeedbackId());
        assertEquals("created", history.getValue().getAction());
        assertEquals("Feedback submitted", history.getValue().getNote());
        assertEquals(OWNER, history.getValue().getChangedBy());
        assertFalse(history.getValue().isDeleted());
        assertNotNull(history.getValue().getChangedAt());

        assertEquals("App idea", response.getSubject());
        assertEquals("Asha Rao", response.getUserName());
        assertEquals("asha@example.com", response.getUserEmail());
        assertEquals("open", response.getStatus());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void addFeedbackNormalisesAnExplicitStatus() {
        FeedbackResponse response = service.addFeedback(session(OWNER, "user"),
                feedbackRequest("Subject", "Bug", "Message", "  In-Review "));

        assertEquals("in-review", response.getStatus());
    }

    @Test
    void addFeedbackWithABlankStatusDefaultsToOpen() {
        FeedbackResponse response = service.addFeedback(session(OWNER, "user"),
                feedbackRequest("Subject", "Bug", "Message", "   "));

        assertEquals("open", response.getStatus());
    }

    @Test
    void addFeedbackWithAnUnknownStatusIsRejected() {
        AuthDirectory.SessionUser user = session(OWNER, "user");
        FeedbackRequest request = feedbackRequest("Subject", "Bug", "Message", "pending");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.addFeedback(user, request));

        assertEquals("Status must be open, in-review, resolved or closed", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
        verify(feedbackHistoryRepository, never()).save(any());
    }

    @Test
    void addFeedbackAcceptsEveryAllowedStatus() {
        for (String status : new String[] {"open", "in-review", "resolved", "closed"}) {
            assertEquals(status, service.addFeedback(session(OWNER, "user"),
                    feedbackRequest("Subject", "Bug", "Message", status)).getStatus());
        }
    }

    @Test
    void ownerCanUpdateEveryFeedbackField() {
        Feedback stored = feedback(ITEM, OWNER, "open", LocalDateTime.of(2026, 1, 1, 0, 0));
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        FeedbackResponse response = service.updateFeedback(session(OWNER, "user"), ITEM,
                feedbackRequest(" New subject ", "Idea", " New message ", "Resolved"));

        assertEquals("New subject", stored.getSubject());
        assertEquals("Idea", stored.getCategory());
        assertEquals("New message", stored.getMessage());
        assertEquals("resolved", stored.getStatus());
        assertEquals("resolved", response.getStatus());
        assertEquals("2026-01-01", response.getCreatedAt());
        verify(feedbackRepository).save(stored);
        ArgumentCaptor<FeedbackHistory> history = ArgumentCaptor.forClass(FeedbackHistory.class);
        verify(feedbackHistoryRepository).save(history.capture());
        assertEquals("updated", history.getValue().getAction());
    }

    @Test
    void updateIgnoresNullFields() {
        Feedback stored = feedback(ITEM, OWNER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        FeedbackResponse response = service.updateFeedback(session(OWNER, "user"), ITEM,
                feedbackRequest(null, null, null, null));

        assertEquals("Old subject", stored.getSubject());
        assertEquals("Bug", stored.getCategory());
        assertEquals("Old message", stored.getMessage());
        assertEquals("open", stored.getStatus());
        assertNull(response.getCreatedAt());
        assertNotNull(stored.getUpdatedAt());
    }

    @Test
    void updateIgnoresBlankFields() {
        Feedback stored = feedback(ITEM, OWNER, "closed", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.updateFeedback(session(OWNER, "user"), ITEM, feedbackRequest("  ", null, "\t", " "));

        assertEquals("Old subject", stored.getSubject());
        assertEquals("Old message", stored.getMessage());
        assertEquals("closed", stored.getStatus());
    }

    @Test
    void updateWithAnInvalidStatusIsRejectedBeforeSaving() {
        Feedback stored = feedback(ITEM, OWNER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));
        AuthDirectory.SessionUser user = session(OWNER, "user");
        FeedbackRequest request = feedbackRequest(null, null, null, "bogus");

        assertThrows(ValidationException.class, () -> service.updateFeedback(user, ITEM, request));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void anAdminCanUpdateSomeoneElsesFeedback() {
        Feedback stored = feedback(ITEM, OTHER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.updateFeedback(session(OWNER, "Admin"), ITEM, feedbackRequest(null, null, null, "closed"));

        assertEquals("closed", stored.getStatus());
    }

    @Test
    void aSuperAdminCanUpdateSomeoneElsesFeedback() {
        Feedback stored = feedback(ITEM, OTHER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.updateFeedback(session(OWNER, "SUPERADMIN"), ITEM, feedbackRequest(null, null, null, "closed"));

        assertEquals("closed", stored.getStatus());
    }

    @Test
    void aMemberCannotUpdateSomeoneElsesFeedback() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM))
                .thenReturn(Optional.of(feedback(ITEM, OTHER, "open", null)));
        AuthDirectory.SessionUser user = session(OWNER, "user");
        FeedbackRequest request = feedbackRequest("x", null, null, null);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> service.updateFeedback(user, ITEM, request));

        assertEquals("You cannot change this feedback", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void aSessionWithoutARoleCannotUpdateSomeoneElsesFeedback() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM))
                .thenReturn(Optional.of(feedback(ITEM, OTHER, "open", null)));
        AuthDirectory.SessionUser user = session(OWNER, null);
        FeedbackRequest request = feedbackRequest("x", null, null, null);

        assertThrows(ForbiddenException.class, () -> service.updateFeedback(user, ITEM, request));
    }

    @Test
    void updatingUnknownFeedbackIsNotFound() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.empty());
        AuthDirectory.SessionUser user = session(OWNER, "user");
        FeedbackRequest request = feedbackRequest("x", null, null, null);

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> service.updateFeedback(user, ITEM, request));
        assertEquals("Feedback not found", exception.getMessage());
    }

    @Test
    void ownerCanDeleteFeedbackWithASoftDelete() {
        Feedback stored = feedback(ITEM, OWNER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.deleteFeedback(session(OWNER, "user"), ITEM);

        assertTrue(stored.isDeleted());
        assertNotNull(stored.getDeletedAt());
        verify(feedbackRepository).save(stored);
        ArgumentCaptor<FeedbackHistory> history = ArgumentCaptor.forClass(FeedbackHistory.class);
        verify(feedbackHistoryRepository).save(history.capture());
        assertEquals("deleted", history.getValue().getAction());
        assertEquals("Feedback removed", history.getValue().getNote());
    }

    @Test
    void anAdminCanDeleteAnyFeedback() {
        Feedback stored = feedback(ITEM, OTHER, "open", null);
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.deleteFeedback(session(OWNER, "admin"), ITEM);

        assertTrue(stored.isDeleted());
    }

    @Test
    void aMemberCannotDeleteSomeoneElsesFeedback() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM))
                .thenReturn(Optional.of(feedback(ITEM, OTHER, "open", null)));
        AuthDirectory.SessionUser user = session(OWNER, "user");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> service.deleteFeedback(user, ITEM));

        assertEquals("You cannot delete this feedback", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void deletingUnknownFeedbackIsNotFound() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.empty());
        AuthDirectory.SessionUser user = session(OWNER, "user");

        assertThrows(ResourceNotFoundException.class, () -> service.deleteFeedback(user, ITEM));
    }

    @Test
    void historyNamesWhoChangedTheFeedback() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM))
                .thenReturn(Optional.of(feedback(ITEM, OWNER, "open", null)));
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(OWNER, "Asha Rao", "active", null)));
        FeedbackHistory known = new FeedbackHistory();
        known.setAction("created");
        known.setNote("Feedback submitted");
        known.setChangedBy(OWNER);
        known.setChangedAt(LocalDateTime.of(2026, 5, 6, 7, 8));
        FeedbackHistory unknown = new FeedbackHistory();
        unknown.setAction("updated");
        unknown.setNote("Feedback updated");
        unknown.setChangedBy(OTHER);
        when(feedbackHistoryRepository.findByFeedbackIdAndDeletedFalseOrderByChangedAtDesc(ITEM))
                .thenReturn(List.of(known, unknown));

        List<FeedbackHistoryResponse> rows = service.history(ITEM);

        assertEquals(2, rows.size());
        assertEquals("created", rows.get(0).getAction());
        assertEquals("Feedback submitted", rows.get(0).getNote());
        assertEquals("2026-05-06", rows.get(0).getAt());
        assertEquals("Asha Rao", rows.get(0).getBy());
        assertNull(rows.get(1).getAt());
        assertEquals("—", rows.get(1).getBy());
    }

    @Test
    void historyOfFeedbackWithoutEntriesIsEmpty() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM))
                .thenReturn(Optional.of(feedback(ITEM, OWNER, "open", null)));
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        when(feedbackHistoryRepository.findByFeedbackIdAndDeletedFalseOrderByChangedAtDesc(ITEM)).thenReturn(List.of());

        assertTrue(service.history(ITEM).isEmpty());
    }

    @Test
    void historyOfUnknownFeedbackIsNotFound() {
        when(feedbackRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.history(ITEM));
        verify(feedbackHistoryRepository, never()).findByFeedbackIdAndDeletedFalseOrderByChangedAtDesc(any());
    }

    @Test
    void allFeedbackShowsOwnerDetailsOrAPlaceholder() {
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(OWNER, "Asha Rao", "active", null)));
        when(feedbackRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(
                feedback(ITEM, OWNER, "open", LocalDateTime.of(2026, 2, 3, 4, 5)),
                feedback(OTHER, UUID.randomUUID(), "closed", null)));

        List<FeedbackResponse> rows = service.allFeedback();

        assertEquals(2, rows.size());
        assertEquals("Asha Rao", rows.get(0).getUserName());
        assertEquals("asha.rao@example.com", rows.get(0).getUserEmail());
        assertEquals("2026-02-03", rows.get(0).getCreatedAt());
        assertEquals("Member", rows.get(1).getUserName());
        assertEquals("", rows.get(1).getUserEmail());
        assertNull(rows.get(1).getCreatedAt());
    }

    @Test
    void allFeedbackCanBeEmpty() {
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        when(feedbackRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());

        assertTrue(service.allFeedback().isEmpty());
    }

    // ------------------------------------------------------------------ news

    @Test
    void publishedNewsGetsAPublishDate() {
        echoNews();

        NewsResponse response = service.addNews(session(OWNER, "admin"),
                newsRequest(" Big update ", " Details ", " HIGH ", " Published "));

        ArgumentCaptor<News> saved = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(saved.capture());
        News news = saved.getValue();
        assertNotNull(news.getId());
        assertEquals(OWNER, news.getAuthorId());
        assertEquals("Big update", news.getTitle());
        assertEquals("Details", news.getBody());
        assertEquals("high", news.getPriority());
        assertEquals("published", news.getStatus());
        assertNotNull(news.getPublishedAt());
        assertFalse(news.isDeleted());
        assertEquals("Big update", response.getTitle());
        assertEquals("Asha Rao", response.getAuthorName());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void newsDefaultsToANormalDraft() {
        echoNews();

        NewsResponse response = service.addNews(session(OWNER, "admin"), newsRequest("Title", "Body", null, null));

        assertEquals("normal", response.getPriority());
        assertEquals("draft", response.getStatus());
        ArgumentCaptor<News> saved = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(saved.capture());
        assertNull(saved.getValue().getPublishedAt());
    }

    @Test
    void newsWithBlankPriorityAndStatusUsesTheDefaults() {
        echoNews();

        NewsResponse response = service.addNews(session(OWNER, "admin"), newsRequest("Title", "Body", " ", " "));

        assertEquals("normal", response.getPriority());
        assertEquals("draft", response.getStatus());
    }

    @Test
    void newsWithoutTitleOrBodyKeepsThemUnset() {
        echoNews();

        NewsResponse response = service.addNews(session(OWNER, "admin"), newsRequest(null, " ", "low", "archived"));

        assertNull(response.getTitle());
        assertNull(response.getBody());
        assertEquals("low", response.getPriority());
        assertEquals("archived", response.getStatus());
    }

    @Test
    void newsWithAnInvalidPriorityIsRejected() {
        AuthDirectory.SessionUser admin = session(OWNER, "admin");
        NewsRequest request = newsRequest("Title", "Body", "urgent", null);

        ValidationException exception = assertThrows(ValidationException.class, () -> service.addNews(admin, request));

        assertEquals("Priority must be low, normal or high", exception.getMessage());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void newsWithAnInvalidStatusIsRejected() {
        AuthDirectory.SessionUser admin = session(OWNER, "admin");
        NewsRequest request = newsRequest("Title", "Body", "low", "scheduled");

        ValidationException exception = assertThrows(ValidationException.class, () -> service.addNews(admin, request));

        assertEquals("Status must be draft, published or archived", exception.getMessage());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void everyAllowedPriorityIsAccepted() {
        echoNews();
        for (String priority : new String[] {"low", "normal", "high"}) {
            assertEquals(priority, service.addNews(session(OWNER, "admin"),
                    newsRequest("Title", "Body", priority, "draft")).getPriority());
        }
    }

    @Test
    void updatingNewsReplacesTheGivenFields() {
        News stored = news(ITEM, "low", "draft", null);
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(OWNER, "Asha Rao", "active", null)));
        echoNews();

        NewsResponse response = service.updateNews(ITEM, newsRequest(" New title ", " New body ", "high", "published"));

        assertEquals("New title", stored.getTitle());
        assertEquals("New body", stored.getBody());
        assertEquals("high", stored.getPriority());
        assertEquals("published", stored.getStatus());
        assertEquals(stored.getUpdatedAt(), stored.getPublishedAt());
        assertEquals("Asha Rao", response.getAuthorName());
        assertEquals("2026-01-02", response.getCreatedAt());
    }

    @Test
    void updatingNewsKeepsTheOriginalPublishDate() {
        LocalDateTime published = LocalDateTime.of(2026, 1, 1, 1, 1);
        News stored = news(ITEM, "normal", "published", published);
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        echoNews();

        NewsResponse response = service.updateNews(ITEM, newsRequest(null, null, null, null));

        assertEquals(published, stored.getPublishedAt());
        assertEquals("Old title", stored.getTitle());
        assertEquals("Old body", stored.getBody());
        assertEquals("normal", stored.getPriority());
        assertEquals("published", stored.getStatus());
        assertEquals("Admin", response.getAuthorName());
    }

    @Test
    void updatingDraftNewsLeavesItUnpublished() {
        News stored = news(ITEM, "normal", "draft", null);
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        echoNews();

        service.updateNews(ITEM, newsRequest(null, null, " ", " "));

        assertNull(stored.getPublishedAt());
        assertEquals("draft", stored.getStatus());
    }

    @Test
    void updatingNewsWithNoStoredPriorityOrStatusFallsBackToDefaults() {
        News stored = news(ITEM, null, null, null);
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        echoNews();

        service.updateNews(ITEM, newsRequest(null, null, null, null));

        assertEquals("normal", stored.getPriority());
        assertEquals("draft", stored.getStatus());
    }

    @Test
    void updatingNewsWithAnInvalidPriorityIsRejected() {
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(news(ITEM, "low", "draft", null)));
        NewsRequest request = newsRequest(null, null, "urgent", null);

        assertThrows(ValidationException.class, () -> service.updateNews(ITEM, request));
        verify(newsRepository, never()).save(any());
    }

    @Test
    void updatingNewsWithAnInvalidStatusIsRejected() {
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(news(ITEM, "low", "draft", null)));
        NewsRequest request = newsRequest(null, null, null, "scheduled");

        assertThrows(ValidationException.class, () -> service.updateNews(ITEM, request));
    }

    @Test
    void updatingUnknownNewsIsNotFound() {
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.empty());
        NewsRequest request = newsRequest("Title", "Body", null, null);

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> service.updateNews(ITEM, request));
        assertEquals("News item not found", exception.getMessage());
    }

    @Test
    void deletingNewsIsASoftDelete() {
        News stored = news(ITEM, "low", "draft", null);
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.of(stored));

        service.deleteNews(ITEM);

        assertTrue(stored.isDeleted());
        assertNotNull(stored.getDeletedAt());
        verify(newsRepository).save(stored);
    }

    @Test
    void deletingUnknownNewsIsNotFound() {
        when(newsRepository.findByIdAndDeletedFalse(ITEM)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteNews(ITEM));
        verify(newsRepository, never()).save(any());
    }

    @Test
    void allNewsShowsAuthorOrAPlaceholder() {
        when(authDirectory.listUsers(null)).thenReturn(List.of(card(OWNER, "Asha Rao", "active", null)));
        News byKnown = news(ITEM, "low", "draft", null);
        News byUnknown = news(OTHER, "low", "draft", null);
        byUnknown.setAuthorId(UUID.randomUUID());
        byUnknown.setCreatedAt(null);
        when(newsRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(byKnown, byUnknown));

        List<NewsResponse> rows = service.allNews();

        assertEquals("Asha Rao", rows.get(0).getAuthorName());
        assertEquals("Admin", rows.get(1).getAuthorName());
        assertNull(rows.get(1).getCreatedAt());
    }

    // ------------------------------------------------------------------ user management

    @Test
    void createAdminForcesTheRoleAndDefaultsTheStatus() {
        UserWriteRequest request = new UserWriteRequest();
        request.setRole("user");
        UserCard created = card(OWNER, "New Admin", "active", null);
        when(authDirectory.createUser(request)).thenReturn(created);

        assertSame(created, service.createAdmin(request));
        assertEquals("admin", request.getRole());
        assertEquals("active", request.getStatus());
    }

    @Test
    void createAdminDefaultsABlankStatus() {
        UserWriteRequest request = new UserWriteRequest();
        request.setStatus("  ");
        when(authDirectory.createUser(request)).thenReturn(new UserCard());

        service.createAdmin(request);

        assertEquals("active", request.getStatus());
    }

    @Test
    void createAdminKeepsAnExplicitStatus() {
        UserWriteRequest request = new UserWriteRequest();
        request.setStatus("inactive");
        when(authDirectory.createUser(request)).thenReturn(new UserCard());

        service.createAdmin(request);

        assertEquals("inactive", request.getStatus());
    }

    @Test
    void updateUserIsDelegatedToTheAuthService() {
        UserWriteRequest request = new UserWriteRequest();
        UserCard updated = card(OWNER, "Updated", "active", null);
        when(authDirectory.updateUser(OWNER, request)).thenReturn(updated);

        assertSame(updated, service.updateUser(OWNER, request));
    }

    @Test
    void updateUserFailuresArePropagated() {
        UserWriteRequest request = new UserWriteRequest();
        when(authDirectory.updateUser(OWNER, request)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(OWNER, request));
    }

    @Test
    void deleteUserIsDelegatedToTheAuthService() {
        service.deleteUser(OWNER);

        verify(authDirectory).deleteUser(OWNER);
    }

    @Test
    void deleteUserFailuresArePropagated() {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found")).when(authDirectory).deleteUser(OWNER);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(OWNER));
    }

    @Test
    void membersAreMarkedWithTheirSetupState() {
        UserCard ready = card(OWNER, "Ready", "active", null);
        UserCard pending = card(OTHER, "Pending", "active", null);
        when(authDirectory.listUsers("user")).thenReturn(List.of(ready, pending));
        when(financeDirectory.usersWithProfile()).thenReturn(List.of(OWNER));

        List<UserCard> members = service.members();

        assertTrue(members.get(0).isSetupComplete());
        assertFalse(members.get(1).isSetupComplete());
    }

    @Test
    void noMembersGivesAnEmptyList() {
        when(authDirectory.listUsers("user")).thenReturn(List.of());
        when(financeDirectory.usersWithProfile()).thenReturn(List.of());

        assertTrue(service.members().isEmpty());
    }

    @Test
    void adminsAreReturnedAsTheyAre() {
        List<UserCard> admins = List.of(card(OWNER, "Root", "active", null));
        when(authDirectory.listUsers("admin")).thenReturn(admins);

        assertSame(admins, service.admins());
    }

    // ------------------------------------------------------------------ workspace

    @Test
    void aMemberWorkspaceHasOnlyTheirOwnData() {
        when(notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(notification(ITEM, OWNER, null)));
        when(feedbackRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(feedback(ITEM, OWNER, "open", null)));
        News published = news(ITEM, "low", "published", null);
        when(newsRepository.findByStatusIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("published"))
                .thenReturn(List.of(published));
        when(authDirectory.listUsers(null)).thenReturn(List.of());

        AdminWorkspaceResponse workspace = service.workspace(OWNER, "user");

        assertEquals(1, workspace.getNotifications().size());
        assertEquals(1, workspace.getFeedback().size());
        assertEquals(1, workspace.getAllFeedback().size());
        assertEquals(1, workspace.getNews().size());
        assertEquals(1, workspace.getAllNews().size());
        assertTrue(workspace.getMembers().isEmpty());
        assertTrue(workspace.getAdmins().isEmpty());
        assertNull(workspace.getAdminInsights());
        verify(feedbackRepository, never()).findByDeletedFalseOrderByCreatedAtDesc();
        verify(newsRepository, never()).findByDeletedFalseOrderByCreatedAtDesc();
        verify(authDirectory, never()).listUsers("user");
    }

    @Test
    void aWorkspaceWithoutARoleIsTreatedAsAMember() {
        when(authDirectory.listUsers(null)).thenReturn(List.of());

        AdminWorkspaceResponse workspace = service.workspace(OWNER, null);

        assertNull(workspace.getAdminInsights());
        assertTrue(workspace.getNotifications().isEmpty());
    }

    @Test
    void anAdminWorkspaceIncludesEverythingAndInsights() {
        String today = LocalDate.now().toString();
        String twoMonthsAgo = LocalDate.now().withDayOfMonth(1).minusMonths(2).toString();
        UserCard activeReady = card(OWNER, "Active Ready", "ACTIVE", today);
        UserCard inactivePending = card(OTHER, "Inactive Pending", "inactive", twoMonthsAgo);
        UserCard noDate = card(UUID.randomUUID(), "No Date", "active", null);
        when(authDirectory.listUsers(null)).thenReturn(List.of(activeReady, inactivePending, noDate));
        when(authDirectory.listUsers("user")).thenReturn(List.of(activeReady, inactivePending, noDate));
        when(authDirectory.listUsers("admin")).thenReturn(List.of(card(UUID.randomUUID(), "Root", "active", today)));
        when(financeDirectory.usersWithProfile()).thenReturn(List.of(OWNER));
        when(feedbackRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER)).thenReturn(List.of());
        when(feedbackRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(
                feedback(ITEM, OWNER, "open", null),
                feedback(OTHER, OWNER, "IN-REVIEW", null),
                feedback(UUID.randomUUID(), OWNER, "resolved", null)));
        when(newsRepository.findByStatusIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("published"))
                .thenReturn(List.of(news(ITEM, "low", "published", null)));
        when(newsRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(
                news(ITEM, "low", "published", null),
                news(OTHER, "low", "draft", null)));

        AdminWorkspaceResponse workspace = service.workspace(OWNER, "Admin");

        assertEquals(3, workspace.getFeedback().size());
        assertEquals(3, workspace.getAllFeedback().size());
        assertEquals(1, workspace.getNews().size());
        assertEquals(2, workspace.getAllNews().size());
        assertEquals(3, workspace.getMembers().size());
        assertEquals(1, workspace.getAdmins().size());

        AdminInsightsResponse insights = workspace.getAdminInsights();
        assertEquals(3, insights.getTotalMembers());
        assertEquals(2, insights.getActiveMembers());
        assertEquals(1, insights.getInactiveMembers());
        assertEquals(1, insights.getAdminCount());
        assertEquals(1, insights.getSetupDone());
        assertEquals(2, insights.getSetupPending());
        assertEquals(3, insights.getFeedbackTotal());
        assertEquals(2, insights.getFeedbackOpen());
        assertEquals(2, insights.getNewsTotal());
        assertEquals(1, insights.getPublishedNews());
        assertEquals(List.of(2, 1), insights.getStatusValues());
        assertEquals(List.of(1, 2), insights.getSetupValues());
        assertEquals(6, insights.getJoinLabels().size());
        assertEquals(LocalDate.now().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                insights.getJoinLabels().get(5));
        assertEquals(List.of(0, 0, 0, 1, 0, 1), insights.getJoinValues());
        assertEquals(3, insights.getRecentMembers().size());
    }

    @Test
    void aSuperAdminWorkspaceAlsoGetsInsights() {
        when(authDirectory.listUsers(null)).thenReturn(List.of());
        when(authDirectory.listUsers("user")).thenReturn(List.of());
        when(authDirectory.listUsers("admin")).thenReturn(List.of());
        when(financeDirectory.usersWithProfile()).thenReturn(List.of());

        AdminWorkspaceResponse workspace = service.workspace(OWNER, "superadmin");

        assertNotNull(workspace.getAdminInsights());
        assertEquals(0, workspace.getAdminInsights().getTotalMembers());
        assertTrue(workspace.getAdminInsights().getRecentMembers().isEmpty());
    }

    @Test
    void recentMembersAreLimitedToEight() {
        List<UserCard> members = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            members.add(card(UUID.randomUUID(), "Member " + index, "active", null));
        }
        when(authDirectory.listUsers(null)).thenReturn(members);
        when(authDirectory.listUsers("user")).thenReturn(members);
        when(authDirectory.listUsers("admin")).thenReturn(List.of());
        when(financeDirectory.usersWithProfile()).thenReturn(List.of());

        AdminInsightsResponse insights = service.workspace(OWNER, "admin").getAdminInsights();

        assertEquals(10, insights.getTotalMembers());
        assertEquals(8, insights.getRecentMembers().size());
        assertEquals(members.get(0), insights.getRecentMembers().get(0));
    }

    @Test
    void authServiceFailuresPropagateOutOfTheWorkspace() {
        when(authDirectory.listUsers(null)).thenThrow(new ValidationException("Could not load users"));
        when(feedbackRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of());

        assertThrows(ValidationException.class, () -> service.workspace(OWNER, "user"));
    }
}
