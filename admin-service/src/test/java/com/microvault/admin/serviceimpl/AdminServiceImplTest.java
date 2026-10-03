package com.microvault.admin.serviceimpl;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.client.FinanceDirectory;
import com.microvault.admin.dto.FeedbackRequest;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.entity.Feedback;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.repository.FeedbackHistoryRepository;
import com.microvault.admin.repository.FeedbackRepository;
import com.microvault.admin.repository.NewsRepository;
import com.microvault.admin.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

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

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void shouldSaveFeedback() {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        user.setFullName("Aarav Sharma");
        user.setEmail("aarav.sharma@example.com");
        user.setRole("user");

        FeedbackRequest request = new FeedbackRequest();
        request.setSubject("App idea");
        request.setCategory("Idea");
        request.setMessage("Please add export");

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feedbackHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse saved = adminService.addFeedback(user, request);

        assertEquals("App idea", saved.getSubject());
        assertEquals("open", saved.getStatus());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void shouldRejectBadFeedbackStatus() {
        AuthDirectory.SessionUser user = new AuthDirectory.SessionUser();
        user.setUserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        user.setRole("user");
        Feedback existing = new Feedback();
        existing.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        existing.setUserId(user.getUserId());
        existing.setStatus("open");
        when(feedbackRepository.findByIdAndDeletedFalse(existing.getId())).thenReturn(Optional.of(existing));

        FeedbackRequest request = new FeedbackRequest();
        request.setStatus("nope");

        assertThrows(ValidationException.class, () -> adminService.updateFeedback(user, existing.getId(), request));
    }

    @Test
    void shouldFailWhenFeedbackIsMissing() {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(feedbackRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.history(id));
    }

    @Test
    void shouldMarkNotificationMissing() {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(notificationRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.markRead(userId, id));
    }
}
