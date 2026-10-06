package com.microvault.admin;

import com.microvault.admin.client.AuthDirectory;
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
import com.microvault.admin.exception.ErrorResponse;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.support.BeanVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every DTO and entity must hand back exactly what was put in through its setters. */
class ModelClassesTest {

    @Test
    void dtosRoundTripEveryProperty() throws Exception {
        List<Class<?>> dtos = List.of(
                AdminInsightsResponse.class, AdminWorkspaceResponse.class, FeedbackHistoryResponse.class,
                FeedbackRequest.class, FeedbackResponse.class, NewsRequest.class, NewsResponse.class,
                NotificationResponse.class, UserCard.class, UserWriteRequest.class,
                AuthDirectory.SessionUser.class);
        for (Class<?> dto : dtos) {
            assertTrue(BeanVerifier.verify(dto) > 0, dto.getSimpleName());
        }
    }

    @Test
    void entitiesRoundTripEveryProperty() throws Exception {
        List<Class<?>> entities = List.of(Feedback.class, FeedbackHistory.class, News.class, Notification.class);
        for (Class<?> entity : entities) {
            assertTrue(BeanVerifier.verify(entity) > 0, entity.getSimpleName());
        }
    }

    @Test
    void insightsStartWithTheDefaultChartLabels() {
        AdminInsightsResponse insights = new AdminInsightsResponse();

        assertEquals(List.of("Active", "Inactive"), insights.getStatusLabels());
        assertEquals(List.of("Setup done", "Pending"), insights.getSetupLabels());
        assertTrue(insights.getJoinLabels().isEmpty());
        assertTrue(insights.getRecentMembers().isEmpty());
    }

    @Test
    void errorResponseCarriesStatusMessageAndPath() {
        ErrorResponse error = ErrorResponse.of(404, "missing", "/api/x");

        assertEquals(404, error.getStatus());
        assertEquals("missing", error.getMessage());
        assertEquals("/api/x", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void exceptionsKeepTheirMessage() {
        assertEquals("a", new ForbiddenException("a").getMessage());
        assertEquals("b", new ResourceNotFoundException("b").getMessage());
        assertEquals("c", new UnauthorizedException("c").getMessage());
        assertEquals("d", new ValidationException("d").getMessage());
    }
}
