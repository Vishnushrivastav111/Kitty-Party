package com.microvault.admin.controller;

import com.microvault.admin.client.AuthDirectory;
import com.microvault.admin.dto.FeedbackHistoryResponse;
import com.microvault.admin.dto.FeedbackRequest;
import com.microvault.admin.dto.FeedbackResponse;
import com.microvault.admin.exception.ForbiddenException;
import com.microvault.admin.exception.ResourceNotFoundException;
import com.microvault.admin.exception.UnauthorizedException;
import com.microvault.admin.exception.ValidationException;
import com.microvault.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    private static final String ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID UUID_ID = UUID.fromString(ID);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuthDirectory authDirectory;

    private AuthDirectory.SessionUser user;

    @BeforeEach
    void setUp() {
        user = new AuthDirectory.SessionUser();
        user.setUserId(UUID_ID);
        user.setFullName("Asha Rao");
        user.setRole("user");
    }

    @Test
    void createReturnsFeedback() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        FeedbackResponse response = new FeedbackResponse();
        response.setSubject("App idea");
        response.setStatus("open");
        when(adminService.addFeedback(eq(user), any(FeedbackRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"App idea\",\"message\":\"Please add export\",\"category\":\"Idea\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("App idea"))
                .andExpect(jsonPath("$.status").value("open"));
        ArgumentCaptor<FeedbackRequest> captor = ArgumentCaptor.forClass(FeedbackRequest.class);
        verify(adminService).addFeedback(eq(user), captor.capture());
        assertEquals("Idea", captor.getValue().getCategory());
    }

    @Test
    void createRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Message is required"));
    }

    @Test
    void createRejectsBlankSubjectAndMessageTogether() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(adminService, never()).addFeedback(any(), any());
    }

    @Test
    void createRejectsAnUnreadableBody() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is not valid"));
    }

    @Test
    void createWithoutALoginIsUnauthorized() throws Exception {
        when(authDirectory.requireSession(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hi\",\"message\":\"There\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login is required"));
        verify(adminService, never()).addFeedback(any(), any());
    }

    @Test
    void createWithAnInvalidStatusIs400() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        when(adminService.addFeedback(eq(user), any()))
                .thenThrow(new ValidationException("Status must be open, in-review, resolved or closed"));

        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hi\",\"message\":\"There\",\"status\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status must be open, in-review, resolved or closed"));
    }

    @Test
    void updateReturnsTheChangedFeedback() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        FeedbackResponse response = new FeedbackResponse();
        response.setStatus("resolved");
        when(adminService.updateFeedback(eq(user), eq(UUID_ID), any(FeedbackRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/feedback/" + ID)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
    }

    @Test
    void updateOfSomeoneElsesFeedbackIsForbidden() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        when(adminService.updateFeedback(eq(user), eq(UUID_ID), any()))
                .thenThrow(new ForbiddenException("You cannot change this feedback"));

        mockMvc.perform(put("/api/feedback/" + ID)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot change this feedback"));
    }

    @Test
    void updateOfUnknownFeedbackIs404() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        when(adminService.updateFeedback(eq(user), eq(UUID_ID), any()))
                .thenThrow(new ResourceNotFoundException("Feedback not found"));

        mockMvc.perform(put("/api/feedback/" + ID)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithoutALoginIsUnauthorized() throws Exception {
        when(authDirectory.requireSession(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(put("/api/feedback/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);

        mockMvc.perform(delete("/api/feedback/" + ID).header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
        verify(adminService).deleteFeedback(user, UUID_ID);
    }

    @Test
    void deleteOfSomeoneElsesFeedbackIsForbidden() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        doThrow(new ForbiddenException("You cannot delete this feedback")).when(adminService).deleteFeedback(user, UUID_ID);

        mockMvc.perform(delete("/api/feedback/" + ID).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOfUnknownFeedbackIs404() throws Exception {
        when(authDirectory.requireSession("Bearer token")).thenReturn(user);
        doThrow(new ResourceNotFoundException("Feedback not found")).when(adminService).deleteFeedback(user, UUID_ID);

        mockMvc.perform(delete("/api/feedback/" + ID).header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutALoginIsUnauthorized() throws Exception {
        when(authDirectory.requireSession(null)).thenThrow(new UnauthorizedException("Login is required"));

        mockMvc.perform(delete("/api/feedback/" + ID)).andExpect(status().isUnauthorized());
    }

    @Test
    void historyListsTheChanges() throws Exception {
        FeedbackHistoryResponse entry = new FeedbackHistoryResponse();
        entry.setAction("created");
        entry.setBy("Asha Rao");
        when(adminService.history(UUID_ID)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/feedback/" + ID + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("created"))
                .andExpect(jsonPath("$[0].by").value("Asha Rao"));
    }

    @Test
    void historyCanBeEmpty() throws Exception {
        when(adminService.history(UUID_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/feedback/" + ID + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void missingFeedbackIsNotFoundOnHistory() throws Exception {
        when(adminService.history(any())).thenThrow(new ResourceNotFoundException("Feedback not found"));

        mockMvc.perform(get("/api/feedback/" + ID + "/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Feedback not found"));
    }
}
